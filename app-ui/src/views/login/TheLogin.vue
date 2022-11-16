<template>
  <div class="login-container">
    <el-form
        ref="ruleFormRef"
        :model="loginForm"
        :rules="rules"
        class="login-form"
        status-icon
    >

      <div class="title-container">
        <h3 class="title">Login</h3>
      </div>

      <el-form-item prop="username">
        <el-input v-model="loginForm.username" autocomplete="off" placeholder="Username" prefix-icon="User"/>
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="loginForm.password" autocomplete="off" placeholder="Password" prefix-icon="Lock"
                  type="password"/>
      </el-form-item>
      <el-form-item>
        <el-button style="width:100%;margin-bottom:30px;" type="primary" @click="handleLogin(ruleFormRef)">Login
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script lang="ts" setup>
import {reactive, ref} from 'vue'
import type {FormInstance, FormRules} from 'element-plus'
import {useRouter} from "vue-router";
import type {Account} from "@/api/types";
import {useUserStore} from "@/stores/user";

const userStore = useUserStore()
const userRouter = useRouter()
const ruleFormRef = ref<FormInstance>()

const loginForm: Account = reactive({
  username: '',
  password: '',
})

const rules = reactive<FormRules>({
  username: [
    {required: true, message: 'Please input Activity username', trigger: 'blur'}
  ],
  password: [
    {required: true, message: 'Please input Activity password', trigger: 'blur'}
  ]
})

const handleLogin = (formEl: FormInstance | undefined) => {
  if (!formEl) return
  formEl.validate((valid) => {
    if (valid) {
      userStore.login(loginForm)
          .then(() => {
            userRouter.push({path: '/home'});
            console.log('login success router to home')
          })
          .catch(() => {
            console.log('login error')
          })
    } else {
      console.log('error submit!')
      return false
    }
  })
}
</script>

<style lang="scss" scoped>
.login-container {
  min-height: 100%;
  width: 100%;
  background-color: #2d3a4b;
  overflow: hidden;

  .login-form {
    position: relative;
    width: 520px;
    max-width: 100%;
    padding: 160px 35px 0;
    margin: 0 auto;
    overflow: hidden;
  }

  .title-container {
    position: relative;

    .title {
      font-size: 26px;
      color: #ced6e0;
      margin: 0 auto 40px auto;
      text-align: center;
      font-weight: bold;
    }
  }

  .input-label {
    color: #ced6e0;
  }
}
</style>
