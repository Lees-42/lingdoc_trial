import { listVaultRepos, getVaultRepo } from '@/api/lingdoc/vault'

const useVaultStore = defineStore(
  'vault',
  {
    state: () => ({
      /** 当前选中的仓库路径（传给后端的 X-Vault-Path） */
      currentVaultPath: localStorage.getItem('lingdoc-current-vault-path') || '',
      /** 当前仓库信息 */
      currentRepo: null,
      /** 用户所有仓库列表 */
      repoList: []
    }),
    getters: {
      /** 当前仓库名称 */
      currentRepoName(state) {
        return state.currentRepo?.repoName || '默认仓库'
      }
    },
    actions: {
      /** 加载用户仓库列表 */
      async loadRepos() {
        try {
          const res = await listVaultRepos()
          if (res.code === 200 && res.data) {
            this.repoList = res.data
            // 如果没有设置当前 vault，使用默认仓库
            if (!this.currentVaultPath) {
              const defaultRepo = res.data.find(r => r.isDefault === '1')
              if (defaultRepo) {
                this.setCurrentVault(defaultRepo)
              } else if (res.data.length > 0) {
                this.setCurrentVault(res.data[0])
              }
            } else {
              // 验证当前路径是否仍有效
              const exist = res.data.find(r => r.repoPath === this.currentVaultPath)
              if (exist) {
                this.currentRepo = exist
              } else {
                // 当前路径已失效，切换到默认仓库
                const defaultRepo = res.data.find(r => r.isDefault === '1')
                if (defaultRepo) {
                  this.setCurrentVault(defaultRepo)
                } else if (res.data.length > 0) {
                  this.setCurrentVault(res.data[0])
                }
              }
            }
          }
          return res
        } catch (e) {
          console.error('加载仓库列表失败', e)
          throw e
        }
      },

      /** 设置当前仓库 */
      setCurrentVault(repo) {
        if (!repo || !repo.repoPath) return
        this.currentVaultPath = repo.repoPath
        this.currentRepo = repo
        localStorage.setItem('lingdoc-current-vault-path', repo.repoPath)
      },

      /** 清空当前仓库（退出登录时调用） */
      clearVault() {
        this.currentVaultPath = ''
        this.currentRepo = null
        this.repoList = []
        localStorage.removeItem('lingdoc-current-vault-path')
      }
    }
  })

export default useVaultStore
