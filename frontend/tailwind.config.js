/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      colors: {
        // Havana 主題色
        havana: {
          green:  '#1a6b3c',
          gold:   '#c9a84c',
          cream:  '#fdf5e4',
          dark:   '#1a1a2e',
        },
      },
    },
  },
  plugins: [],
}


