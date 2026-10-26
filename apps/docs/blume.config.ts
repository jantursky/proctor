import { readFileSync } from 'node:fs'

import { defineConfig } from 'blume'

const properties = readFileSync(new URL('../../gradle.properties', import.meta.url), 'utf8')
const version = /^VERSION_NAME=(.+)$/m.exec(properties)?.[1]?.trim()
if (!version) throw new Error('VERSION_NAME is missing from gradle.properties')

export default defineConfig({
  title: 'Proctor',
  description:
    'Evaluation harness for on-device system LLMs: one eval suite for Gemini Nano and Apple Foundation Models, from Kotlin Multiplatform.',
  logo: '/logo.svg',
  github: {
    owner: 'jantursky',
    repo: 'proctor',
    dir: 'apps/docs',
  },
  navigation: {
    actions: [{ href: 'https://central.sonatype.com/artifact/io.github.jantursky/proctor', label: 'Maven Central' }],
  },
  content: {
    root: 'docs',
  },
  theme: {
    accent: '#155dfc',
    radius: 'md',
    mode: 'system',
  },
  lastModified: 'git',
  variables: {
    version,
  },
})
