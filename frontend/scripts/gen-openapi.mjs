/* eslint-disable no-console */
import fs from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import openapiTS, { astToString } from 'openapi-typescript'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const SOURCE = process.env.OPENAPI_SOURCE || 'http://localhost:9080/v3/api-docs'
const OUTPUT = path.resolve(__dirname, '..', 'src', 'types', 'openapi.d.ts')

async function main() {
  console.log(`[openapi] generating types from ${SOURCE}`)
  let input: string | URL
  try {
    new URL(SOURCE)
    input = new URL(SOURCE)
  } catch {
    input = SOURCE
  }
  const ast = await openapiTS(input as URL, {
    additionalProperties: false,
    immutable: false
  })
  const banner = `/* eslint-disable */\n/**\n * AUTO GENERATED FILE - DO NOT EDIT.\n * Run \`npm run gen:openapi\` to regenerate.\n * Source: ${SOURCE}\n */\n\n`
  const content = banner + astToString(ast)
  await fs.mkdir(path.dirname(OUTPUT), { recursive: true })
  await fs.writeFile(OUTPUT, content, 'utf8')
  console.log(`[openapi] wrote ${OUTPUT}`)
}

main().catch((err) => {
  console.error('[openapi] generation failed:', err)
  process.exit(1)
})
