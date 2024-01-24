FROM hseeberger/scala-sbt:17.0.2_1.6.2_3.1.1 AS build

WORKDIR /build
COPY build.sbt .
COPY project ./project/
COPY core ./core
COPY example/product/src ./example/product/src
EXPOSE 4001
CMD sbt example-product/run
