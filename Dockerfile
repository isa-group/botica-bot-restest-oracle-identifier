FROM ubuntu:22.04
RUN rm -f /bin/sh && ln -s /bin/bash /bin/sh

#=======================================================================
# Daikon Build
#=======================================================================

RUN export DEBIAN_FRONTEND=noninteractive \
    && apt-get -qqy update \
    && apt-get -qqy install \
      autoconf \
      automake \
      bc \
      binutils-dev \
      gcc \
      git \
      graphviz \
      jq \
      m4 \
      make \
      rsync \
      unzip \
      wget \
      openjdk-17-jdk-headless `# bot java version` \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# This Daikon version requires JDK 8
ARG JDK_VERSION=8.302.08.1
ARG JDK_URL=https://corretto.aws/downloads/resources/${JDK_VERSION}/amazon-corretto-${JDK_VERSION}-linux-x64.tar.gz
RUN mkdir -p /usr/lib/jvm \
    && wget -qO /tmp/jdk.tar.gz ${JDK_URL} \
    && tar -xzf /tmp/jdk.tar.gz -C /usr/lib/jvm \
    && rm /tmp/jdk.tar.gz

ENV JAVA_8_HOME=/usr/lib/jvm/amazon-corretto-${JDK_VERSION}-linux-x64
ENV PATH=${JAVA_8_HOME}/bin:${PATH}

# Clone and build Daikon from source
WORKDIR /build
ARG BUILD_DIR=/build
ARG DAIKON_BUILD_DIR=${BUILD_DIR}/daikon_modified

RUN mkdir -p ${BUILD_DIR} \
    && git clone https://github.com/mimbrero/daikon_modified/ --depth=1 ${DAIKON_BUILD_DIR} \
    && chmod -R +x ${DAIKON_BUILD_DIR} \
    && source ${DAIKON_BUILD_DIR}/scripts/daikon.bashrc \
    && make -C ${DAIKON_BUILD_DIR} compile \
    && make -C ${DAIKON_BUILD_DIR} daikon.jar \
    && mkdir -p /app \
    && mv ${DAIKON_BUILD_DIR}/daikon.jar /app/daikon.jar \
    && rm -rf ${BUILD_DIR}

## Set up Daikon environment variables
ENV DAIKON_JAVA_PATH=${JAVA_8_HOME}/bin/java
ENV DAIKON_EXEC_PATH=/app/daikon.jar

#=======================================================================
# Bot Build
#=======================================================================

# Switch to OpenJDK 17 for the bot application
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=${JAVA_HOME}/bin:${PATH}

# Set up bot application
WORKDIR /app
COPY target/bot.jar /app/bot.jar

CMD ["java", "-jar", "/app/bot.jar"]
