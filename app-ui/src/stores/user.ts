import {defineStore} from 'pinia'
import type {Account} from "@/api/system/types";
import {login} from "@/api/system/account";

export const useUserStore = defineStore({
  id: 'user',
  state: () => ({
    counter: 0,
  }),
  getters: {
    doubleCount: (state) => state.counter * 2,
  },
  actions: {
    login(loginForm: Account) {
      localStorage.setItem("", "")
      return new Promise((resolve, reject) => {
        login(loginForm).then(resp => {
          resolve(resp.data)
        }).catch(error => {
          reject(error)
        })
      })
    },
  },
})
