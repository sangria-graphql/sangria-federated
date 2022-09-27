const {ApolloGateway} = require('@apollo/gateway');
const {ApolloServerPluginInlineTrace} = require("apollo-server-core");
const {Server} = require('./server');


const gateway = new ApolloGateway({
  serviceList: [
    { name: 'states', url: 'http://localhost:9081/api/graphql'},
    { name: 'reviews', url: 'http://localhost:9082/api/graphql'}
  ],
  debug: true
});

const port = 9080;

(async () => {

  const server = new Server({
    gateway,
    subscriptions: false,
    plugins: [ApolloServerPluginInlineTrace()],
  });

  server.listen({ port: 9080 }).then(({url}) => {
    console.log(`🚀 Server ready at ${url}`)
  })
})();
