import { defineConfig, loadEnv } from 'vite'
import path from 'path'
import createVitePlugins from './vite/plugins'

const baseUrl = 'http://localhost:8080' // 后端接口

// https://vitejs.dev/config/
export default defineConfig(({ mode, command }) => {
  const env = loadEnv(mode, process.cwd())
  const { VITE_APP_ENV } = env
  return {
    // 部署生产环境和开发环境下的URL。
    // 默认情况下，vite 会假设你的应用是被部署在一个域名的根路径上
    // 例如 https://www.lingdoc.vip/。如果应用被部署在一个子路径上，你就需要用这个选项指定这个子路径。例如，如果你的应用被部署在 https://www.lingdoc.vip/admin/，则设置 baseUrl 为 /admin/。
    base: VITE_APP_ENV === 'production' ? '/' : '/',
    plugins: createVitePlugins(env, command === 'build'),
    resolve: {
      // https://cn.vitejs.dev/config/#resolve-alias
      alias: {
        // 设置路径
        '~': path.resolve(__dirname, './'),
        // 设置别名
        '@': path.resolve(__dirname, './src')
      },
      // https://cn.vitejs.dev/config/#resolve-extensions
      extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue'] // 模块解析扩展名设置
    },
    // 打包配置
    build: {
      // https://vite.dev/config/build-options.html
      sourcemap: command === 'build' ? false : 'inline', // 源码映射
      outDir: 'dist', // 输出目录
      assetsDir: 'assets', // 静态资源目录(dist/assets)
      chunkSizeWarningLimit: 2000,
      rollupOptions: { // 打包选项
        output: {
          chunkFileNames: 'static/js/[name]-[hash].js',
          entryFileNames: 'static/js/[name]-[hash].js',
          assetFileNames: 'static/[ext]/[name]-[hash].[ext]' // 静态资源文件名
        }
      }
    },
    // 配置 Vite 开发服务器
    server: {
      port: 3000,
      host: true, // 允许外部访问
      open: true, // 自动打开浏览器
      server: {
    allowedHosts: ['prerespectable-abstractively-kim.ngrok-free.dev']
  },
  /*
  监听规则：捕获所有以 /dev-api 开头的请求
  转发目标：转给 http://localhost:8080
  路径改写：去掉 /dev-api 再转发 
  */
    proxy: {
      // https://cn.vitejs.dev/config/#server-proxy
      '/dev-api': {
        target: baseUrl,
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/dev-api/, '')
      },
        // springdoc proxy
        '^/v3/api-docs/(.*)': {
        target: baseUrl,
        changeOrigin: true,
      }
    }
},
    css: {
      postcss: {
        plugins: [
          {
            postcssPlugin: 'internal:charset-removal',
            AtRule: {
              charset: (atRule) => {
                if (atRule.name === 'charset') {
                  atRule.remove()
                }
              }
            }
          }
        ]
      }
    }
  }
})
