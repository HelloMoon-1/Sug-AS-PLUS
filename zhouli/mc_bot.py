#!/usr/bin/env python3
"""
Minecraft Bot — 静默加入正版服务器，收发消息，保活。

Target: protocol 774 (MC 26.1.2)
依赖: pip install requests pycryptodome

Usage:
  export MC_TOKEN="eyJ..."    # 正版 accessToken (优先级1)
  python mc_bot.py mc.hypixel.net            # 也自动读启动器 token
  python mc_bot.py mc.hypixel.net --msa      # 交互式浏览器登录
  python mc_bot.py localhost --offline       # 离线模式
"""

import argparse
import asyncio
import json
import os
import socket
import struct
import sys
import uuid as uuid_mod
from datetime import datetime
from pathlib import Path

import requests

TOKEN_CACHE = Path(__file__).parent / "mc_bot_token.json"

# ── Packet ID (protocol 774) ──────────────────────────
LOGIN_C2S = {"hello":0x00,"key":0x01,"custom_answer":0x02,"acknowledged":0x03,"cookie_response":0x04}
LOGIN_S2C = {0x00:"disconnect",0x01:"hello",0x02:"login_finished",0x03:"compression",0x04:"custom_query",0x05:"cookie_request"}
PLAY_C2S = {"chat_ack":0x06,"chat_command":0x07,"chat_signed":0x08,"chat":0x09,"chat_session":0x0A,"keep_alive":0x1C}
PLAY_S2C = {0x20:"disconnect",0x2C:"keep_alive",0x41:"player_chat",0x79:"system_chat"}


# ═══════════════════════════════════════════════════════
#  协议工具
# ═══════════════════════════════════════════════════════

class Buf:
    __slots__=('data','off')
    def __init__(self,d=b''):self.data=bytearray(d);self.off=0
    def remaining(self):return len(self.data)-self.off
    def read(self,n):
        if self.off+n>len(self.data):raise EOFError()
        r=bytes(self.data[self.off:self.off+n]);self.off+=n;return r
    def read_varint(self):
        v=0
        for i in range(5):
            b=self.read(1)[0];v|=(b&0x7F)<<(7*i)
            if not(b&0x80):return v
        raise ValueError()
    def read_string(self):return self.read(self.read_varint()).decode('utf-8')
    def read_uuid(self):return uuid_mod.UUID(bytes=self.read(16))
    def write(self,b):self.data.extend(b)
    def write_varint(self,v):
        while True:
            b=v&0x7F;v>>=7
            if v:b|=0x80
            self.data.append(b)
            if not v:break
    def write_string(self,s):
        r=s.encode('utf-8');self.write_varint(len(r));self.data.extend(r)
    def pack(self):return bytes(self.data)
    def prepend_varint(self):
        p=self.pack();self.data=bytearray();self.write_varint(len(p));self.data.extend(p);return self.pack()


# ═══════════════════════════════════════════════════════
#  加密
# ═══════════════════════════════════════════════════════

def rsa_encrypt(d,pk):
    from Crypto.PublicKey import RSA
    from Crypto.Cipher import PKCS1_v1_5
    return PKCS1_v1_5.new(RSA.import_key(pk)).encrypt(d)
def aes_cfb8_pair(key):
    from Crypto.Cipher import AES
    return AES.new(key,AES.MODE_CFB,key,segment_size=8).encrypt, AES.new(key,AES.MODE_CFB,key,segment_size=8).decrypt


# ═══════════════════════════════════════════════════════
#  认证
# ═══════════════════════════════════════════════════════

def find_launcher_token():
    bases = []
    if sys.platform=="win32":
        bases.append(Path(os.environ["APPDATA"])/".minecraft")
        bases.append(Path(os.environ["APPDATA"])/"Minecraft Launcher")
    bases+=[Path.home()/"Library/Application Support/minecraft",Path.home()/".minecraft"]
    for b in bases:
        for fn in ("launcher_accounts.json","launcher_accounts_microsoft_store.json"):
            fp=b/fn
            if fp.exists():
                try:
                    for acct in json.loads(fp.read_text("utf-8")).get("accounts",{}).values():
                        t=acct.get("minecraftAccessToken") or acct.get("accessToken","")
                        if t: return t
                except: pass
    for b in [Path(os.environ.get("APPDATA",""))/".minecraft",Path.home()/".minecraft"]:
        fp=b/"launcher_profiles.json"
        if fp.exists():
            try:
                for db in json.loads(fp.read_text("utf-8")).get("authenticationDatabase",{}).values():
                    t=db.get("accessToken","") or db.get("token","")
                    if t: return t
            except: pass
    return None


MSA_CLIENT_ID = "00000000402b5328"   # 旧 Minecraft 启动器（唯一能过 Minecraft API 的）

def msa_browser_auth():
    """Live Connect 授权码流程（这个 client id 只认 Live Connect 端点）"""
    import webbrowser, urllib.parse

    redirect = "https://login.live.com/oauth20_desktop.srf"
    auth_url = (
        f"https://login.live.com/oauth20_authorize.srf"
        f"?client_id={MSA_CLIENT_ID}&scope=XboxLive.signin&response_type=code"
        f"&redirect_uri={urllib.parse.quote(redirect,safe='')}"
    )

    print("[*] 浏览器登录...")
    print("  登录微软账号后浏览器跳转到空白页")
    print("  复制地址栏完整 URL 粘贴回来即可")
    print(f"  打开浏览器...")
    webbrowser.open(auth_url)
    print(f"  备用 URL: {auth_url}")
    line = input("  粘贴重定向后的 URL: ").strip()

    qs = urllib.parse.parse_qs(urllib.parse.urlparse(line).query)
    if "code" not in qs:
        qs = urllib.parse.parse_qs(urllib.parse.urlparse(line).fragment)
    if "code" not in qs:
        print(f"  [!] URL 中没有 code 参数")
        sys.exit(1)

    code = qs["code"][0]
    hdr = {"Content-Type":"application/x-www-form-urlencoded"}

    # Live Connect v1 token 端点
    r = requests.post("https://login.live.com/oauth20_token.srf",
        data={"client_id":MSA_CLIENT_ID,"code":code,"redirect_uri":redirect,
              "grant_type":"authorization_code"},
        headers=hdr)
    if r.status_code!=200:
        print(f"  [!] Token 交换失败: {r.status_code} {r.text[:200]}")
        sys.exit(1)

    return _xbl_flow(r.json()["access_token"])

def _xbl_flow(msa_token):
    h={"Content-Type":"application/json","Accept":"application/json"}
    r=requests.post("https://user.auth.xboxlive.com/user/authenticate",
        json={"Properties":{"AuthMethod":"RPS","SiteName":"user.auth.xboxlive.com","RpsTicket":f"d={msa_token}"},
              "RelyingParty":"http://auth.xboxlive.com","TokenType":"JWT"},headers=h)
    r.raise_for_status();j=r.json();uhs=j["DisplayClaims"]["xui"][0]["uhs"];xbl=j["Token"]

    r=requests.post("https://xsts.auth.xboxlive.com/xsts/authorize",
        json={"Properties":{"SandboxId":"RETAIL","UserTokens":[xbl]},
              "RelyingParty":"rp://api.minecraftservices.com/","TokenType":"JWT"},headers=h)
    r.raise_for_status();xsts=r.json()["Token"]

    r=requests.post("https://api.minecraftservices.com/authentication/login_with_xbox",
        json={"identityToken":f"XBL3.0 x={uhs};{xsts}"},headers=h)
    if r.status_code!=200:
        print(f"  [!] Minecraft 登录失败: HTTP {r.status_code} {r.text[:200]}")
        sys.exit(1)

    mc=r.json()["access_token"]
    r=requests.get("https://api.minecraftservices.com/minecraft/profile",
        headers={"Authorization":f"Bearer {mc}"})
    r.raise_for_status();p=r.json()
    print(f"[+] 认证通过: {p['name']}")
    return {"token":mc,"uuid":p["id"],"name":p["name"]}


def _auth_result(d):
    """把 dict 转成 (token, uuid_str, name)  tuple，处理 UUID 格式"""
    uid = d["uuid"]
    if len(uid) == 36:  # 带连字符
        uid = uid.replace("-", "")
    return d["token"], uid, d["name"]

def _save_cache(token,uuid_str,name):
    try:
        TOKEN_CACHE.write_text(json.dumps({"token":token,"uuid":uuid_str,"name":name,"time":datetime.now().isoformat()}))
    except Exception as e:
        print(f"  [!] 保存 token 缓存失败: {e}")

def _load_cache():
    try:
        if TOKEN_CACHE.exists():
            return json.loads(TOKEN_CACHE.read_text())
    except: pass
    return None

def authenticate(offline=False,force_msa=False):
    if offline:
        name=os.environ.get("USERNAME") or os.environ.get("USER") or "Steve"
        print(f"[*] 离线模式: {name}")
        return None,uuid_mod.uuid4().hex[:16],name

    # 优先级: 环境变量 > 缓存文件 > 启动器 token > 浏览器登录
    token = os.environ.get("MC_TOKEN")
    if not token:
        cached = _load_cache()
        if cached:
            token = cached["token"]

    if not token:
        token = find_launcher_token()

    if token:
        r=requests.get("https://api.minecraftservices.com/minecraft/profile",headers={"Authorization":f"Bearer {token}"})
        if r.status_code==200:
            p=r.json(); result=(token,p["id"].replace("-",""),p["name"])
            _save_cache(*result); return result
        print("[*] Token 无效或过期，需要重新登录")

    result=_auth_result(msa_browser_auth())
    _save_cache(result[0],result[1],result[2])
    return result


# ═══════════════════════════════════════════════════════
#  Bot
# ═══════════════════════════════════════════════════════

class McBot:
    def __init__(self,host,port,token,uuid_str,username):
        self.host=host;self.port=port;self.token=token;self.username=username
        self.uuid=uuid_mod.UUID(uuid_str) if uuid_str else uuid_mod.UUID(int=0)
        self.reader=self.writer=None;self._enc_fn=self._dec_fn=None;self._compress=-1;self._running=True;self.on_chat=None

    async def _read_raw(self,n):
        d=b''
        while len(d)<n:
            c=await self.reader.read(n-len(d))
            if not c: raise ConnectionError("断开")
            if self._dec_fn: c=self._dec_fn(c)
            d+=c
        return d

    async def _read_varint(self):
        v=0
        for i in range(5):
            b=(await self._read_raw(1))[0];v|=(b&0x7F)<<(7*i)
            if not(b&0x80):return v
        raise ValueError()

    async def _read_packet(self):
        plen=await self._read_varint();data=await self._read_raw(plen)
        if self._compress>0:
            b=Buf(data);ul=b.read_varint();import zlib
            data=zlib.decompress(bytes(b.data[b.off:])) if ul>0 else bytes(b.data[b.off:])
        b=Buf(data);return b.read_varint(),bytes(b.data[b.off:])

    async def _write_raw(self,data):
        if self._enc_fn: data=self._enc_fn(data)
        self.writer.write(data);await self.writer.drain()

    async def _write_packet(self,pid,payload=b''):
        p=Buf();p.write_varint(pid);p.data.extend(payload);await self._write_raw(p.prepend_varint())

    async def handshake(self,ns=2):
        p=Buf();p.write_varint(774)
        p.write_string(self.host.encode('idna').decode('ascii'));p.write_varint(self.port);p.write_varint(ns)
        await self._write_packet(0x00,p.pack())

    async def login_start(self):
        p=Buf();p.write_string(self.username[:16]);p.write(self.uuid.bytes)
        await self._write_packet(LOGIN_C2S["hello"],p.pack())

    async def login(self):
        await self.handshake(2);await self.login_start()
        while True:
            pid,data=await self._read_packet();buf=Buf(data)
            if pid==0x01:
                sid_str=buf.read_string()          # serverId (用于 hash)
                pk=buf.read(buf.read_varint())     # publicKey
                vt=buf.read(buf.read_varint())     # challenge
                if buf.remaining(): buf.read(1)    # shouldAuthenticate
                ss=os.urandom(16);resp=Buf()
                for b in(rsa_encrypt(ss,pk),rsa_encrypt(vt,pk)):
                    resp.write_varint(len(b));resp.write(b)
                await self._write_packet(LOGIN_C2S["key"],resp.pack())
                self._enc_fn,self._dec_fn=aes_cfb8_pair(ss)
                # 会话认证: POST sessionserver.mojang.com
                import hashlib as _hl
                h=_hl.sha1();h.update(sid_str.encode('ISO-8859-1'));h.update(ss);h.update(pk)
                val=int.from_bytes(h.digest(),'big',signed=True)
                sid=hex(val)[2:] if val>=0 else '-'+hex(val)[3:]
                print(f"  [DEBUG] sid={sid} sid_str={sid_str!r}")
                r=requests.post("https://sessionserver.mojang.com/session/minecraft/join",
                    json={"accessToken":self.token,"selectedProfile":self.uuid.hex,"serverId":sid},
                    timeout=5)
                if r.status_code!=204:
                    print(f"  [!] 会话认证失败: {r.status_code} {r.text[:100]}")
                    return False
                print("[*] 加密启用 + 会话认证成功")
            elif pid==0x02:
                uid=buf.read_uuid();name=buf.read_string()
                for _ in range(buf.read_varint()):buf.read_string();buf.read_string()
                if buf.read(1)[0]:buf.read_string()
                print(f"[+] {name} 已加入");await self._write_packet(LOGIN_C2S["acknowledged"]);return True
            elif pid==0x03: self._compress=buf.read_varint();print(f"[*] 压缩 {self._compress}")
            elif pid==0x04:
                mid=buf.read_varint();ch=buf.read_string();rest=bytes(buf.data[buf.off:])
                r=Buf();r.write_varint(mid);r.write(b'\x00' if rest else b'')
                await self._write_packet(LOGIN_C2S["custom_answer"],r.pack())
            elif pid==0x00:print(f"[-] 登录被拒: {buf.read_string()}");return False

    async def play_loop(self):
        while self._running:
            try: pid,data=await self._read_packet()
            except(ConnectionError,EOFError,OSError)as e:print(f"[-] 断开: {e}");break
            buf=Buf(data)
            if pid==0x2C:
                kid=struct.unpack('>q',buf.read(8))[0];await self._write_packet(PLAY_C2S["keep_alive"],struct.pack('>q',kid))
            elif pid==0x41:
                try:
                    buf.read_uuid();buf.read_varint()
                    if buf.read(1)[0]:sl=buf.read_varint();buf.read(sl)
                    body=Buf(buf.read(buf.read_varint()));c=body.read_string()
                    print(f"[CHAT] {c}")
                    if self.on_chat:await self.on_chat(c)
                except Exception as e:print(f"[?] chat: {e}")
            elif pid==0x79:
                try:
                    cj=buf.read_string();ov=buf.read(1)[0]!=0
                    if not ov:
                        comp=json.loads(cj);parts=[]
                        if isinstance(comp,dict):
                            parts.append(comp.get("text",""))
                            for ex in comp.get("extra",[]):
                                parts.append(ex.get("text","") if isinstance(ex,dict) else str(ex))
                        else:parts.append(str(comp))
                        t="".join(parts);print(f"[SYS] {t}")
                        if self.on_chat:await self.on_chat(t)
                except Exception as e:print(f"[?] sys: {e}")
            elif pid==0x20:print(f"[-] 被踢: {buf.read_string()}");break

    async def send_chat(self,msg):
        now=datetime.now();p=Buf();p.write_string(msg)
        p.data.extend(struct.pack('>q',int(now.timestamp())));p.data.extend(struct.pack('>i',now.microsecond*1000))
        p.data.extend(struct.pack('>q',0))  # salt
        p.write(b'\x00')                    # signature = null
        p.write_varint(0)                   # offset
        p.data.extend(b'\x00\x00\x00')     # acknowledged BitSet (20 bits = 3 bytes)
        p.data.append(0)                    # checksum
        await self._write_packet(PLAY_C2S["chat"],p.pack())

    async def send_command(self,cmd):
        p=Buf();p.write_string(cmd);await self._write_packet(PLAY_C2S["chat_command"],p.pack())

    @staticmethod
    def _resolve_host(host):
        """解析域名，系统 DNS 失败则 fallback 到 DoH"""
        # 系统 DNS
        try:
            addrs = socket.getaddrinfo(host, 0, socket.AF_INET, socket.SOCK_STREAM)
            if addrs: return addrs[0][4][0]
        except Exception:
            pass
        # DoH 备用
        for doh_url in (
            f"https://cloudflare-dns.com/dns-query?name={host}&type=A",
            f"https://dns.quad9.net/dns-query?name={host}&type=A",
            f"https://dns.google/resolve?name={host}&type=A",
        ):
            try:
                r = requests.get(doh_url, headers={"Accept": "application/dns-json"}, timeout=5)
                if r.status_code == 200:
                    for a in r.json().get("Answer", []):
                        if a.get("type") == 1:
                            return a["data"]
            except Exception:
                continue
        print(f"  [!] DNS 解析失败: {host} 不存在或网络无法访问")
        return None

    async def run(self):
        print(f"[*] 连接 {self.host}:{self.port} ...")
        ip = self._resolve_host(self.host)
        if ip is None:
            return
        if ip != self.host:
            print(f"  DNS: {self.host} -> {ip}")
        try: self.reader,self.writer=await asyncio.wait_for(asyncio.open_connection(ip,self.port),10)
        except OSError as e:
            print(f"[-] 无法连接: {e}")
            if "getaddrinfo" in str(e):
                print("  提示: 服务器地址可能错误，请检查拼写")
            return
        except Exception as e:print(f"[-] {e}");return
        print("[*] 登录...")
        if not await self.login():self.writer.close();return
        print("[+] 已加入, 输入消息发送, Ctrl+C 退出")
        play=asyncio.create_task(self.play_loop())
        loop=asyncio.get_event_loop()

        async def stdin():
            while self._running:
                line=await loop.run_in_executor(None,sys.stdin.readline)
                if not line:break
                msg=line.rstrip('\n')
                try:
                    if msg.startswith('/'):await self.send_command(msg[1:])
                    else:await self.send_chat(msg)
                except Exception as e:print(f"[!] {e}")

        sin=asyncio.create_task(stdin())
        await asyncio.wait([play,sin],return_when=asyncio.FIRST_COMPLETED)
        self._running=False;self.writer.close()


# ═══════════════════════════════════════════════════════
#  入口
# ═══════════════════════════════════════════════════════

def main():
    ap=argparse.ArgumentParser(description="MC Bot protocol 774")
    ap.add_argument("server");ap.add_argument("port",nargs="?",type=int,default=4001)
    ap.add_argument("--msa",action="store_true");ap.add_argument("--offline",action="store_true")
    args=ap.parse_args()
    token,ustr,name=authenticate(args.offline,args.msa)
    print(f"  用户: {name}")
    bot=McBot(args.server,args.port,token,ustr,name)
    try: asyncio.run(bot.run())
    except KeyboardInterrupt: print("\n[!] 中断")

if __name__=="__main__":
    main()
