import js from '@eslint/js';
import globals from 'globals';
import tseslint from 'typescript-eslint';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import prettier from 'eslint-config-prettier';

// ESLint 9 flat config（Lab 5 Step 2「代码质量基础设施」）
export default tseslint.config(
  {
    // 构建产物与依赖不参与检查
    ignores: ['dist', 'node_modules', '*.tsbuildinfo'],
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  {
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2021,
      globals: { ...globals.browser },
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
    },
    rules: {
      // hooks 依赖数组与调用位置：这两条最能拦住真实 bug
      'react-hooks/rules-of-hooks': 'error',
      'react-hooks/exhaustive-deps': 'warn',
      // 组件文件只导出组件，保证热更新边界干净
      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
      // 显式忽略未使用参数时用 _ 前缀，其余未使用变量报错
      '@typescript-eslint/no-unused-vars': [
        'error',
        { argsIgnorePattern: '^_', varsIgnorePattern: '^_' },
      ],
    },
  },
  // 放在最后：关闭与 Prettier 冲突的格式类规则
  prettier,
);
