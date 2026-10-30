// Builds the demo site (the sample report with a sticky header) into demo-dist/.
//   node scripts/build-demo.mjs
// The report only follows the system colour scheme, so its dark palette is also keyed on <html data-theme>.
import { cpSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs'

const out = new URL('../demo-dist/', import.meta.url)
const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

const DOCS = 'https://proctor-docs-phi.vercel.app'
const GITHUB = 'https://github.com/jantursky/proctor'

let html = read('../docs/samples/report/index.html')
const dark = /@media \(prefers-color-scheme:dark\)\{:root\{([^}]*)\}\}/.exec(html)
if (!dark) throw new Error('demo: the dark palette of the report was not found')
html = html.replace(
  dark[0],
  `:root[data-theme=dark]{${dark[1]};color-scheme:dark}:root[data-theme=light]{color-scheme:light}` +
    `@media (prefers-color-scheme:dark){:root:not([data-theme=light]){${dark[1]};color-scheme:dark}}`,
)

const style = `
header.site{position:sticky;top:0;z-index:10;background:var(--canvas);border-bottom:1px solid var(--hairline)}
header.site .bar{max-width:1200px;margin:0 auto;padding:10px 16px;display:flex;align-items:center;gap:16px}
header.site a{color:var(--ink);text-decoration:none}
header.site .brand{display:flex;align-items:center;gap:10px;font-weight:700;font-size:18px;margin-right:auto}
header.site .brand img{width:28px;height:28px}
header.site nav{display:flex;align-items:center;gap:6px}
header.site nav a,header.site button{display:inline-flex;align-items:center;gap:6px;padding:6px 10px;border-radius:8px;border:0;background:none;color:var(--body);font:inherit;font-weight:600;cursor:pointer}
header.site nav a:hover,header.site button:hover{background:var(--band);color:var(--ink)}
header.site svg{width:18px;height:18px;fill:none;stroke:currentColor;stroke-width:2;stroke-linecap:round;stroke-linejoin:round}
header.site svg.fill{fill:currentColor;stroke:none}
html:not([data-theme=dark]) .moon,html[data-theme=dark] .sun{display:none}
@media (prefers-color-scheme:dark){html:not([data-theme]) .moon{display:none}html:not([data-theme]) .sun{display:inline}}
@media (max-width:520px){header.site .label{display:none}}
`

const header = `<header class="site"><div class="bar">
<a class="brand" href="/"><img src="mark.svg" alt="">Proctor</a>
<nav>
<a href="${DOCS}"><svg viewBox="0 0 24 24"><path d="M4 5a2 2 0 0 1 2-2h12v18H6a2 2 0 0 1-2-2zM4 19a2 2 0 0 1 2-2h12"/></svg><span class="label">Docs</span></a>
<a href="${GITHUB}"><svg class="fill" viewBox="0 0 24 24"><path d="M12 .5a11.5 11.5 0 0 0-3.64 22.41c.58.1.79-.25.79-.56v-2c-3.2.7-3.88-1.37-3.88-1.37-.52-1.33-1.28-1.69-1.28-1.69-1.05-.71.08-.7.08-.7 1.15.08 1.76 1.19 1.76 1.19 1.03 1.76 2.69 1.25 3.35.96.1-.75.4-1.25.73-1.54-2.55-.29-5.24-1.28-5.24-5.69 0-1.26.45-2.28 1.19-3.09-.12-.29-.52-1.46.11-3.05 0 0 .97-.31 3.17 1.18a11 11 0 0 1 5.77 0c2.2-1.49 3.17-1.18 3.17-1.18.63 1.59.23 2.76.11 3.05.74.81 1.19 1.83 1.19 3.09 0 4.42-2.7 5.4-5.27 5.68.41.36.78 1.06.78 2.14v3.17c0 .31.21.67.8.56A11.5 11.5 0 0 0 12 .5z"/></svg><span class="label">GitHub</span></a>
<button type="button" id="theme" aria-label="Toggle light and dark theme"><svg class="sun" viewBox="0 0 24 24"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/></svg><svg class="moon" viewBox="0 0 24 24"><path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/></svg></button>
</nav></div></header>`

const script = `<script>
(function () {
  var root = document.documentElement
  try { var saved = localStorage.getItem('theme'); if (saved) root.dataset.theme = saved } catch (e) {}
  document.getElementById('theme').addEventListener('click', function () {
    var dark = root.dataset.theme ? root.dataset.theme === 'dark' : matchMedia('(prefers-color-scheme:dark)').matches
    var next = dark ? 'light' : 'dark'
    root.dataset.theme = next
    try { localStorage.setItem('theme', next) } catch (e) {}
  })
})()
</script>`

// Applied before first paint so a saved choice does not flash.
const early = `<script>try{var t=localStorage.getItem('theme');if(t)document.documentElement.dataset.theme=t}catch(e){}</script>`

html = html
  .replace('</style>', `${style}</style>${early}`)
  .replace('<body>', `<body>${header}`)
  .replace('</body>', `${script}</body>`)
  .replace('<title>Proctor report</title>', '<title>Proctor demo</title>')

rmSync(out, { recursive: true, force: true })
mkdirSync(out, { recursive: true })
writeFileSync(new URL('index.html', out), html)
cpSync(new URL('../docs/samples/report/report.md', import.meta.url), new URL('report.md', out))
cpSync(new URL('../design/brand/mark.svg', import.meta.url), new URL('mark.svg', out))
console.log('demo built into demo-dist/')
