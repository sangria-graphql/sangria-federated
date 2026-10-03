FROM sbtscala/scala-sbt:eclipse-temurin-17_2.x AS build

WORKDIR /build
COPY build.sbt .
COPY project ./project/
COPY core ./core
COPY example/product/src ./example/product/src
EXPOSE 4001
CMD sbt example-product/run
