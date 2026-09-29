# 基础镜像
FROM crpi-i8oro01k2vwf5t1y.cn-chengdu.personal.cr.aliyuncs.com/yh-namespace/eclipse-temurin:8-jre-jammy

# 作者
MAINTAINER xiaofuge

# 配置
ENV PARAMS=""

# 时区
ENV TZ=PRC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 添加应用
ADD target/s-pay-mall-ddd-yh-app.jar /s-pay-mall-ddd-yh-app.jar

ENTRYPOINT ["sh","-c","java -jar $JAVA_OPTS /s-pay-mall-ddd-yh-app.jar $PARAMS"]