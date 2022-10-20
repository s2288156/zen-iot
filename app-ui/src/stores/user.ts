import {defineStore} from 'pinia'
import type {Account} from "@/api/system/types";
import {login} from "@/api/system/account";

export const useUserStore = defineStore({
  id: 'user',
  state: () => ({
    counter: 0,
  }),
  getters: {},
  actions: {
    login(loginForm: Account) {
      return new Promise((resolve, reject) => {
        login(loginForm).then(resp => {
          localStorage.setItem("UserAuthentication", resp.data.data.token)
          resolve(resp.data)
        }).catch(error => {
          reject(error)
        })
      })
    },
    logout() {
      return new Promise((resolve, reject) => {
        localStorage.clear()
        resolve(null)
      })
    },
  },
})
