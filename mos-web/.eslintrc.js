module.exports = {
  "extends": [require.resolve('@umijs/lint/dist/config/eslint')],
  "globals": {
    "React": "readonly",
    "UMI": "readonly",
    "request": "readonly",
    "history": "readonly",
    "useModel": "readonly",
    "useAccess": "readonly"
  },
  "rules": {
    "@typescript-eslint/no-explicit-any": "off",
    "@typescript-eslint/no-unused-vars": ["warn", { "argsIgnorePattern": "^_" }],
    "no-console": "off"
  },
  "ignorePatterns": ["src/.umi", "src/.umi-production", "dist", "node_modules"]
}
