import { defineStore } from 'pinia'
import { appModules, AppState } from '@/store/store-data'

export const useAppStore = defineStore('app', {
  state: (): AppState => appModules,
  getters: {
    getHeaderContent (): string {
      return this.headerContent
    }
  }
})
