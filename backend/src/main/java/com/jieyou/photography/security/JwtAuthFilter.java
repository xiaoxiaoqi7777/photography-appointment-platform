package com.jieyou.photography.security;

import com.jieyou.photography.module.user.entity.User;
import com.jieyou.photography.module.user.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器：解析 Authorization 头，校验令牌并加载用户
 * （同时校验用户状态，依据 API 约定 3.3.5），失败则不设置认证，由后续逻辑返回 401。
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserMapper userMapper;

    public JwtAuthFilter(JwtService jwtService, UserMapper userMapper) {
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        SecurityContextHolder.clearContext();
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            try {
                Claims claims = jwtService.parse(header.substring(BEARER_PREFIX.length()));
                Object uidClaim = claims.get("uid");
                if (uidClaim instanceof Number uid) {
                    User user = userMapper.selectById(uid.longValue());
                    if (user != null && user.getStatus() != null && user.getStatus() == 1) {
                        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), user.getRole());
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                loginUser, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (Exception ex) {
                log.debug("JWT 校验失败: {}", ex.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}
