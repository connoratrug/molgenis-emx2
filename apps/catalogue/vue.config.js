const HOST = process.env.MOLGENIS_APPS_HOST || "http://localhost:8080";
const SCHEMA = process.env.MOLGENIS_APPS_SCHEMA || "Conception";

module.exports = {
  publicPath: "", // to ensure relative paths are used
  chainWebpack: (config) => {
    // Run vue 3 in vue 2 mode
    config.resolve.alias.set("vue", "@vue/compat");
    config.module
      .rule("vue")
      .use("vue-loader")
      .tap((options) => {
        return {
          ...options,
          compilerOptions: {
            compatConfig: {
              MODE: 3,
            },
          },
        };
      });
    // GraphQL Loader, allows import of .gql files
    config.module
      .rule("graphql")
      .test(/\.(graphql|gql)$/)
      .use("webpack-graphql-loader")
      .loader("webpack-graphql-loader")
      .end();
  },
  devServer: {
    port: "9090",
    proxy: {
      "^/graphql": {
        target: `${HOST}/${SCHEMA}`,
      },
      "/api": { target: `${HOST}` },
      "/apps": { target: `${HOST}` },
      "^/theme.css": { target: `${HOST}/${SCHEMA}` },
    },
  },
};
