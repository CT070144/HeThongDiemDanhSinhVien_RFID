// config-overrides.js
const webpack = require('webpack');

module.exports = function override(config, env) {
  config.devServer = config.devServer || {};
  config.devServer.allowedHosts = 'all'; // Cho phép mọi domain (ngrok, IP, v.v.)
  return config;
};
module.exports = {
    webpack: (config) => config,
    devServer: (config) => {
      config.allowedHosts = 'all';
      // với webpack-dev-server cũ: config.disableHostCheck = true;
      return config;
    },
  };
