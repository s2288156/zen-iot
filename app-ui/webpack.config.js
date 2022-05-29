const AutoImport = require('unplugin-auto-import/dist/webpack');
const Components = require('unplugin-vue-components/dist/webpack');
const { ElementPlusResolver } = require('unplugin-vue-components/dist/resolvers');

module.exports = {
  // ...
  plugins: [
    AutoImport({
      resolvers: [ElementPlusResolver()],
    }),
    Components({
      resolvers: [ElementPlusResolver()],
    }),
  ],
};
