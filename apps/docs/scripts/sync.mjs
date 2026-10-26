// Copies files that live elsewhere in the repo into the docs site before a build.
//  - CHANGELOG.md          -> docs/_generated/changelog.md (without its top-level title)
//  - docs/samples/report/* -> public/sample-report/* (both targets are gitignored)
import { cpSync, existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const root = new URL('../../../', import.meta.url)
const here = new URL('../', import.meta.url)
const path = (url) => fileURLToPath(url)

const changelog = new URL('CHANGELOG.md', root)
if (!existsSync(changelog)) {
  console.error(`sync: ${path(changelog)} is missing. The docs changelog page is generated from it.`)
  process.exit(1)
}
const lines = readFileSync(changelog, 'utf8').split('\n')
const body = lines[0]?.startsWith('# ') ? lines.slice(1).join('\n').trimStart() : lines.join('\n')
mkdirSync(new URL('docs/_generated/', here), { recursive: true })
writeFileSync(new URL('docs/_generated/changelog.md', here), body)

const report = new URL('docs/samples/report/', root)
if (existsSync(report)) {
  cpSync(path(report), path(new URL('public/sample-report/', here)), { recursive: true })
} else {
  console.warn('sync: docs/samples/report/ not found, the sample report link will be dead.')
}
