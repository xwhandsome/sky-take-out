package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义切面，实现公共字段自动填充
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    /**
     * 切入点
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut(){}

    /**
     * 前置通知，在通知中填充公共字段
     */
    @Before("autoFillPointCut()")
    public void autofill(JoinPoint joinPoint){
        log.info("开始进行公共字段填充...");

        // 获取当前被拦截的方法上的数据库操作类型
        MethodSignature signature= (MethodSignature) joinPoint.getSignature(); //获取方法签名对象
        AutoFill autofull = signature.getMethod().getAnnotation(AutoFill.class); //获得方法上的注解对象
        OperationType operationType = autofull.value();//获得数据库操作类型

        // 获取方法参数，实体对象
        Object[] args = joinPoint.getArgs();
        if(args == null || args.length == 0){
            return;
        }

        Object entity = args[0];
        // 准备赋值的数据
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        // 根据对应的数据库操作类型，为对应的字段赋值
        if (operationType == OperationType.INSERT) {
            try {
                // 1. 尝试填充 CreateTime (几乎所有实体都有)
                try {
                    Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                    setCreateTime.invoke(entity, now);
                } catch (NoSuchMethodException ignored) {}

                // 2. 尝试填充 CreateUser (User类可能没有)
                try {
                    Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                    setCreateUser.invoke(entity, currentId);
                } catch (NoSuchMethodException ignored) {}

                // 3. 尝试填充 UpdateTime
                try {
                    Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                    setUpdateTime.invoke(entity, now);
                } catch (NoSuchMethodException ignored) {}

                // 4. 尝试填充 UpdateUser
                try {
                    Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                    setUpdateUser.invoke(entity, currentId);
                } catch (NoSuchMethodException ignored) {}

            } catch (Exception e) {
                log.error("公共字段填充失败: {}", e.getMessage());
            }
        }

        else if (operationType == OperationType.UPDATE){
            //2个字段需要赋值
            try {
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                // 通过反射为对象属性赋值
                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, currentId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
    }


}
