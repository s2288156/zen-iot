import {defineStore} from 'pinia'
import type {Account} from "@/api/data/types";
import {login} from "@/api/account-apis";

const TOKEN_KEY = 'UserAuthentication'

export const useUserStore = defineStore({
  id: 'user',
  state: () => ({
    token: ''
  }),
  getters: {},
  actions: {
    login(loginForm: Account) {
      return new Promise((resolve, reject) => {
        login(loginForm).then(resp => {
          localStorage.setItem(TOKEN_KEY, resp.data.token)
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
    getToken() {
      return localStorage.getItem(TOKEN_KEY)
    }
  },
})
