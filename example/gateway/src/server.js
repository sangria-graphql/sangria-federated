const {ApolloServer} = require('apollo-server');
const costAnalysis = require('graphql-cost-analysis').default;

class Server extends ApolloServer {
  async createGraphQLServerOptions(
    req, res
  ) {
    const options = await super.createGraphQLServerOptions(req, res);

    return {
      ...options,
      validationRules: [
        costAnalysis({
          maximumCost: 750,
          defaultCost: 1,
          variables: req.body.variables,
          onComplete(cost) {
            console.log(`Query complexity is: ${cost}`)
          }
        }),
      ],
    };
  }
};

module.exports = {Server} ;