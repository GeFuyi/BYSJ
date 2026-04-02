package com.community.config;

import com.community.security.JwtFilter;
import com.community.security.JwtRealm;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class ShiroConfig {

    @Bean
    public SecurityManager securityManager(JwtRealm jwtRealm) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(jwtRealm);

        DefaultSubjectDAO subjectDAO = (DefaultSubjectDAO) securityManager.getSubjectDAO();
        DefaultSessionStorageEvaluator sessionStorageEvaluator = new DefaultSessionStorageEvaluator();
        sessionStorageEvaluator.setSessionStorageEnabled(false);
        subjectDAO.setSessionStorageEvaluator(sessionStorageEvaluator);
        SecurityUtils.setSecurityManager(securityManager);

        return securityManager;
    }

    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean(SecurityManager securityManager) {
        ShiroFilterFactoryBean filterFactoryBean = new ShiroFilterFactoryBean();
        filterFactoryBean.setSecurityManager(securityManager);
        Map<String, javax.servlet.Filter> filters = new LinkedHashMap<>();
        filters.put("jwt", new JwtFilter());
        filterFactoryBean.setFilters(filters);

        Map<String, String> chainDefinitionMap = new LinkedHashMap<>();
        chainDefinitionMap.put("/api/auth/**", "anon");
        chainDefinitionMap.put("/api/health", "anon");
        chainDefinitionMap.put("/api/repair/file", "anon");
        chainDefinitionMap.put("/api/services/file", "anon");
        chainDefinitionMap.put("/api/social/file", "anon");
        chainDefinitionMap.put("/api/electricity/notify", "anon");
        chainDefinitionMap.put("/ws/**", "anon");
        chainDefinitionMap.put("/error", "anon");
        chainDefinitionMap.put("/**", "jwt");
        filterFactoryBean.setFilterChainDefinitionMap(chainDefinitionMap);
        return filterFactoryBean;
    }

    @Bean
    public FilterRegistrationBean<Filter> shiroFilterRegistration(ShiroFilterFactoryBean shiroFilterFactoryBean) throws Exception {
        FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter((Filter) shiroFilterFactoryBean.getObject());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setName("shiroFilter");
        registrationBean.setOrder(1);
        return registrationBean;
    }

    @Bean
    public LifecycleBeanPostProcessor lifecycleBeanPostProcessor() {
        return new LifecycleBeanPostProcessor();
    }

    @Bean
    public DefaultAdvisorAutoProxyCreator advisorAutoProxyCreator() {
        DefaultAdvisorAutoProxyCreator creator = new DefaultAdvisorAutoProxyCreator();
        creator.setProxyTargetClass(true);
        return creator;
    }

    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager);
        return advisor;
    }
}
