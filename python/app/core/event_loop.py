"""uvicorn 的自定义事件循环工厂。

为什么需要这个文件：
    psycopg3 的异步模式不支持 Windows 默认的 ProactorEventLoop，会报
    "Psycopg cannot use the 'ProactorEventLoop' to run in async mode"，
    表现是连接池一个连接都拿不到、30 秒后 PoolTimeout 启动失败。

为什么不用 asyncio.set_event_loop_policy：
    uvicorn 0.54 起在 main.py/server.py 里用 asyncio_run(loop_factory=...) 建循环，
    loop_factory 直接返回 asyncio.ProactorEventLoop（见 uvicorn/loops/asyncio.py），
    绕过了 policy。所以在 import 时设 policy 不起作用，只能接管工厂。

用法：
    uvicorn.run("main:app", loop="app.core.event_loop:selector_loop_factory")
    或命令行 --loop app.core.event_loop:selector_loop_factory
"""

import asyncio
import selectors


def selector_loop_factory(use_subprocess: bool = False):
    """创建一个 SelectorEventLoop 并直接返回。

    注意返回值必须是 loop 实例，不能是「返回 loop 的函数」：
    自定义 loop 字符串走的是 Config.get_loop_factory 的 else 分支，
    它把这函数原样交给 asyncio.Runner，由 Runner 无参调用一次。
    如果这里再返回一个 lambda，Runner 拿到的是函数，会报
    "'function' object has no attribute 'create_task'"。
    """
    return asyncio.SelectorEventLoop(selectors.SelectSelector())