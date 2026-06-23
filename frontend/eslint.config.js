// ESLint配置（ESLint v9+ 扁平配置格式）
import js from '@eslint/js'
import vue from 'eslint-plugin-vue'
import ts from '@typescript-eslint/eslint-plugin'
import tsParser from '@typescript-eslint/parser'
import vueParser from 'vue-eslint-parser'
import globals from 'globals'

export default [
  // 基础JS推荐配置
  js.configs.recommended,
  // 全局变量定义
  {
    files: ['**/*.js', '**/*.ts', '**/*.vue'],
    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.node,
        AMap: 'readonly',
        localStorage: 'readonly',
        sessionStorage: 'readonly',
        FormData: 'readonly',
        HTMLElement: 'readonly',
        NodeJS: 'readonly'
      }
    }
  },
  // Vue推荐配置（扁平格式）
  ...vue.configs['flat/recommended'],
  // TypeScript配置
  {
    files: ['**/*.ts', '**/*.vue'],
    plugins: {
      '@typescript-eslint': ts
    },
    languageOptions: {
      parser: vueParser,
      parserOptions: {
        parser: tsParser,
        ecmaVersion: 'latest',
        sourceType: 'module'
      }
    },
    rules: {
      '@typescript-eslint/no-explicit-any': 'warn',
      '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }]
    }
  },
  // 自定义规则
  {
    files: ['**/*.vue', '**/*.ts', '**/*.js'],
    rules: {
      // Vue规则
      'vue/multi-word-component-names': 'off',
      'vue/no-v-html': 'warn',
      'vue/require-default-prop': 'off',
      'vue/no-setup-props-destructure': 'off',
      
      // 通用规则
      'no-console': 'warn',
      'no-debugger': 'warn',
      'no-alert': 'error',
      'prefer-const': 'error',
      'eqeqeq': ['error', 'always'],
      'curly': ['error', 'all'],
      'quotes': ['error', 'single', { avoidEscape: true }],
      'semi': ['error', 'never']
    }
  }
]