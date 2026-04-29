# -*- coding: utf-8 -*-
"""
=============================================================================
LingDoc AI Service - Authentication Middleware
=============================================================================
用途：验证主后端发来的请求是否合法，防止未授权访问

安全模型：
  - AI 服务不直接面向外网用户，只接收来自主后端的内部请求
  - 主后端通过 HTTP Header "X-Internal-Token" 携带共享密钥
  - AI 服务校验此 Token 是否与配置一致，不一致则拒绝

对接说明（给主项目工程师）：
  - 主后端每次调用 AI 服务时，必须在请求头中携带：
    X-Internal-Token: {config.AI_INTERNAL_TOKEN 的值}
  - 缺少此 Header 或值不匹配，AI 服务会返回 401 Unauthorized
  - 建议主后端在 AiProxyClient 中统一封装此 Header

安全建议：
  - 生产环境 Token 应 ≥ 32 位随机字符串
  - 定期轮换（建议每 3 个月）
  - 内网通信 + Token 双重防护，不暴露 AI 服务到外网
=============================================================================
"""

from fastapi import Request, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from app.config import config


# 使用 HTTPBearer 作为安全方案，FastAPI 会自动在 /docs 中显示认证按钮
security = HTTPBearer(auto_error=False)


async def verify_internal_token(
    request: Request,
    credentials: HTTPAuthorizationCredentials = None
) -> str:
    """
    验证内部共享密钥
    
    用途：校验请求是否来自信任的主后端
    
    参数：
        request: FastAPI 请求对象，从中提取 X-Internal-Token Header
        credentials: HTTPBearer 提取的凭证（备用，优先用自定义 Header）
    
    返回：
        str: 验证通过的 Token 值
    
    异常：
        HTTPException(401): Token 缺失或不匹配
    
    对接说明（给主项目工程师）：
      - 主后端发送请求时的代码示例：
        ```java
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", aiServiceToken);
        HttpEntity<DocProcessRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<DocProcessResponse> response = restTemplate.exchange(
            aiServiceUrl + "/api/ai/v1/doc/process",
            HttpMethod.POST,
            entity,
            DocProcessResponse.class
        );
        ```
    """
    # 优先从自定义 Header 读取（更符合直觉）
    token = request.headers.get("X-Internal-Token")
    
    # 备用：从 Authorization Bearer 读取（兼容标准 OAuth）
    if not token and credentials:
        token = credentials.credentials
    
    # 检查 Token 是否存在
    if not token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={
                "error": "UNAUTHORIZED",
                "message": "缺少认证信息，请携带 X-Internal-Token Header",
                "hint": "主后端需在请求头中设置 X-Internal-Token: {your_token}"
            },
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    # 检查 Token 是否匹配
    if token != config.AI_INTERNAL_TOKEN:
        # 安全审计日志（不暴露正确 Token）
        from app.utils.logger import logger
        logger.warning(
            f"认证失败：收到非法 Token 请求 | "
            f"client_ip={request.client.host if request.client else 'unknown'} | "
            f"path={request.url.path} | "
            f"token_prefix={token[:8]}..."
        )
        
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={
                "error": "INVALID_TOKEN",
                "message": "认证失败：Token 不匹配",
                "hint": "请检查 AI_INTERNAL_TOKEN 配置是否一致"
            },
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    return token


class InternalTokenAuth:
    """
    内部认证类，供路由装饰器使用
    
    用途：简化路由层的认证调用
    
    使用方式：
        ```python
        from app.middleware.auth import InternalTokenAuth
        auth = InternalTokenAuth()
        
        @router.post("/doc/process")
        async def process_doc(request: DocProcessRequest, token: str = Depends(auth)):
            # token 已验证通过，继续处理业务逻辑
            pass
        ```
    
    对接说明（给主项目工程师）：
      - 所有 /api/ai/v1/* 接口都需要此认证
      - /health 健康检查接口不需要认证（方便监控）
    """
    
    async def __call__(self, request: Request) -> str:
        """FastAPI Depends 调用入口"""
        return await verify_internal_token(request)


# 预创建认证实例，路由层直接导入使用
internal_auth = InternalTokenAuth()
