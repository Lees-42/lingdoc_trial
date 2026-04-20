import cache from '@/plugins/cache'
import useSettingsStore from '@/store/modules/settings'

const PERSIST_KEY = 'tags-view-visited'

function resolveTitle(view) {
  if (view && view.meta && view.meta.title) return view.meta.title
  if (view && view.title) return view.title
  if (view && view.path === '/index') return '首页'
  if (view && view.name) return view.name
  if (view && view.path) return view.path
  return 'no-name'
}

function getViewKey(view) {
  return view.fullPath || view.path || view.name || ''
}

function normalizeView(view) {
  const meta = view && view.meta ? { ...view.meta } : {}
  return {
    ...view,
    path: view && view.path ? view.path : '',
    fullPath: view && view.fullPath ? view.fullPath : (view && view.path) ? view.path : '',
    name: view && view.name ? view.name : '',
    query: view && view.query ? view.query : undefined,
    meta,
    title: resolveTitle(view)
  }
}

function mergeView(currentView, incomingView) {
  return normalizeView({
    ...currentView,
    ...incomingView,
    meta: {
      ...(currentView && currentView.meta ? currentView.meta : {}),
      ...(incomingView && incomingView.meta ? incomingView.meta : {})
    },
    title: resolveTitle(incomingView) !== 'no-name' ? resolveTitle(incomingView) : resolveTitle(currentView)
  })
}

function normalizeVisitedViews(views) {
  const normalized = []
  ;(views || []).forEach(view => {
    const currentView = normalizeView(view)
    if (!currentView.path) return
    const index = normalized.findIndex(item => getViewKey(item) === getViewKey(currentView) || item.path === currentView.path)
    if (index === -1) {
      normalized.push(currentView)
      return
    }
    normalized[index] = mergeView(normalized[index], currentView)
  })
  return normalized
}

function isPersistEnabled() {
  return useSettingsStore().tagsViewPersist
}

function saveVisitedViews(views) {
  if (!isPersistEnabled()) return
  const toSave = normalizeVisitedViews(views).filter(v => !(v.meta && v.meta.affix))
  cache.local.setJSON(PERSIST_KEY, toSave)
}

function loadVisitedViews() {
  return normalizeVisitedViews(cache.local.getJSON(PERSIST_KEY) || [])
}

function clearVisitedViews() {
  cache.local.remove(PERSIST_KEY)
}

const useTagsViewStore = defineStore(
  'tags-view',
  {
    state: () => ({
      visitedViews: [],
      cachedViews: [],
      iframeViews: []
    }),
    actions: {
      addView(view) {
        this.addVisitedView(view)
        this.addCachedView(view)
      },
      addIframeView(view) {
        const normalizedView = normalizeView(view)
        if (this.iframeViews.some(v => v.path === normalizedView.path)) return
        this.iframeViews.push(
          normalizedView
        )
      },
      addVisitedView(view) {
        const normalizedView = normalizeView(view)
        if (!normalizedView.path) return
        const index = this.visitedViews.findIndex(v => getViewKey(v) === getViewKey(normalizedView) || v.path === normalizedView.path)
        if (index > -1) {
          this.visitedViews.splice(index, 1, mergeView(this.visitedViews[index], normalizedView))
          saveVisitedViews(this.visitedViews)
          return
        }
        this.visitedViews.push(normalizedView)
        saveVisitedViews(this.visitedViews)
      },
      addAffixView(view) {
        const normalizedView = normalizeView(view)
        if (!normalizedView.path) return
        const index = this.visitedViews.findIndex(v => getViewKey(v) === getViewKey(normalizedView) || v.path === normalizedView.path)
        if (index > -1) {
          this.visitedViews.splice(index, 1)
        }
        this.visitedViews.unshift(normalizedView)
      },
      addCachedView(view) {
        if (this.cachedViews.includes(view.name)) return
        if (!view.meta.noCache) {
          this.cachedViews.push(view.name)
        }
      },
      delView(view) {
        return new Promise(resolve => {
          this.delVisitedView(view)
          this.delCachedView(view)
          resolve({
            visitedViews: [...this.visitedViews],
            cachedViews: [...this.cachedViews]
          })
        })
      },
      delVisitedView(view) {
        return new Promise(resolve => {
          for (const [i, v] of this.visitedViews.entries()) {
            if (v.path === view.path) {
              this.visitedViews.splice(i, 1)
              break
            }
          }
          this.iframeViews = this.iframeViews.filter(item => item.path !== view.path)
          saveVisitedViews(this.visitedViews)
          resolve([...this.visitedViews])
        })
      },
      delIframeView(view) {
        return new Promise(resolve => {
          this.iframeViews = this.iframeViews.filter(item => item.path !== view.path)
          resolve([...this.iframeViews])
        })
      },
      delCachedView(view) {
        return new Promise(resolve => {
          const index = this.cachedViews.indexOf(view.name)
          index > -1 && this.cachedViews.splice(index, 1)
          resolve([...this.cachedViews])
        })
      },
      delOthersViews(view) {
        return new Promise(resolve => {
          this.delOthersVisitedViews(view)
          this.delOthersCachedViews(view)
          resolve({
            visitedViews: [...this.visitedViews],
            cachedViews: [...this.cachedViews]
          })
        })
      },
      delOthersVisitedViews(view) {
        return new Promise(resolve => {
          this.visitedViews = this.visitedViews.filter(v => {
            return v.meta.affix || v.path === view.path
          })
          this.iframeViews = this.iframeViews.filter(item => item.path === view.path)
          saveVisitedViews(this.visitedViews)
          resolve([...this.visitedViews])
        })
      },
      delOthersCachedViews(view) {
        return new Promise(resolve => {
          const index = this.cachedViews.indexOf(view.name)
          if (index > -1) {
            this.cachedViews = this.cachedViews.slice(index, index + 1)
          } else {
            this.cachedViews = []
          }
          resolve([...this.cachedViews])
        })
      },
      delAllViews(view) {
        return new Promise(resolve => {
          this.delAllVisitedViews(view)
          this.delAllCachedViews(view)
          resolve({
            visitedViews: [...this.visitedViews],
            cachedViews: [...this.cachedViews]
          })
        })
      },
      delAllVisitedViews(view) {
        return new Promise(resolve => {
          const affixTags = this.visitedViews.filter(tag => tag.meta.affix)
          this.visitedViews = affixTags
          this.iframeViews = []
          clearVisitedViews()
          resolve([...this.visitedViews])
        })
      },
      delAllCachedViews(view) {
        return new Promise(resolve => {
          this.cachedViews = []
          resolve([...this.cachedViews])
        })
      },
      updateVisitedView(view) {
        for (let v of this.visitedViews) {
          if (v.path === view.path) {
            v = Object.assign(v, view)
            break
          }
        }
      },
      delRightTags(view) {
        return new Promise(resolve => {
          const index = this.visitedViews.findIndex(v => v.path === view.path)
          if (index === -1) {
            return
          }
          this.visitedViews = this.visitedViews.filter((item, idx) => {
            if (idx <= index || (item.meta && item.meta.affix)) {
              return true
            }
            const i = this.cachedViews.indexOf(item.name)
            if (i > -1) {
              this.cachedViews.splice(i, 1)
            }
            if(item.meta.link) {
              const fi = this.iframeViews.findIndex(v => v.path === item.path)
              this.iframeViews.splice(fi, 1)
            }
            return false
          })
          saveVisitedViews(this.visitedViews)
          resolve([...this.visitedViews])
        })
      },
      delLeftTags(view) {
        return new Promise(resolve => {
          const index = this.visitedViews.findIndex(v => v.path === view.path)
          if (index === -1) {
            return
          }
          this.visitedViews = this.visitedViews.filter((item, idx) => {
            if (idx >= index || (item.meta && item.meta.affix)) {
              return true
            }
            const i = this.cachedViews.indexOf(item.name)
            if (i > -1) {
              this.cachedViews.splice(i, 1)
            }
            if(item.meta.link) {
              const fi = this.iframeViews.findIndex(v => v.path === item.path)
              this.iframeViews.splice(fi, 1)
            }
            return false
          })
          saveVisitedViews(this.visitedViews)
          resolve([...this.visitedViews])
        })
      },
      // 恢复持久化的 tags
      loadPersistedViews() {
        const views = loadVisitedViews()
        views.forEach(view => {
          this.addVisitedView(view)
        })
      }
    }
  })

export default useTagsViewStore
