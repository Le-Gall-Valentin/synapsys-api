import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import boundaries from 'eslint-plugin-boundaries'

export default tseslint.config(
  { ignores: ['dist'] },
  {
    extends: [js.configs.recommended, ...tseslint.configs.recommended],
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
    },
  },
  {
    plugins: { boundaries },
    settings: {
      'import/resolver': {
        typescript: { alwaysTryTypes: true },
      },
      'boundaries/elements': [
        { type: 'shared',   pattern: ['src/shared/**'] },
        { type: 'entities', pattern: ['src/entities/**'] },
        { type: 'features', pattern: ['src/features/*/**'], capture: ['feature'] },
        { type: 'pages',    pattern: ['src/pages/*/**'],    capture: ['page'] },
        { type: 'app',      pattern: ['src/app/**'] },
      ],
      'boundaries/ignore': ['src/main.tsx', 'src/vite-env.d.ts'],
    },
    rules: {
      'boundaries/element-types': ['error', {
        default: 'disallow',
        rules: [
          { from: 'shared',   allow: ['shared'] },
          { from: 'entities', allow: ['shared'] },
          {
            from: 'features',
            allow: [
              'shared',
              'entities',
              ['features', { feature: '${feature}' }],
            ],
          },
          {
            from: 'pages',
            allow: [
              'shared',
              'entities',
              'features',
              ['pages', { page: '${page}' }],
            ],
          },
          { from: 'app', allow: ['shared', 'entities', 'features', 'pages', 'app'] },
        ],
      }],
    },
  },
)