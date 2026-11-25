/**
 * uni-app 的 vue-loader 用模块名 require('@vue/component-compiler-utils')，
 * 在干净安装下会解析到 vue-cli-service 自带的 3.3.0（不含 recyclableRender 补丁），
 * 导致 H5 模板编译报 "Export 'recyclableRender' is not defined"。
 * 改为指向 uni-app 自带的 3.1.0 补丁版，确保 CI 与本地表现一致。
 */
const fs = require('fs')
const path = require('path')

const loaderPath = path.join(
  __dirname,
  '..',
  'node_modules',
  '@dcloudio',
  'vue-cli-plugin-uni',
  'packages',
  'vue-loader',
  'lib',
  'loaders',
  'templateLoader.js'
)
const original = "require('@vue/component-compiler-utils')"
const patched = "require('../../../@vue/component-compiler-utils')"

if (!fs.existsSync(loaderPath)) {
  console.warn('[patch-uni-vue-loader] templateLoader.js not found, skip')
  process.exit(0)
}

const source = fs.readFileSync(loaderPath, 'utf8')
if (source.includes(patched)) {
  process.exit(0)
}
if (!source.includes(original)) {
  console.warn('[patch-uni-vue-loader] expected require not found, skip')
  process.exit(0)
}

fs.writeFileSync(loaderPath, source.replace(original, patched))
console.log('[patch-uni-vue-loader] patched uni template loader')
