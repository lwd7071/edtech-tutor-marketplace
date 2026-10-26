#!/usr/bin/env node
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

const root = resolve(fileURLToPath(new URL('..', import.meta.url)));
const frontendRequire = createRequire(resolve(root, 'frontend/package.json'));
const ts = frontendRequire('typescript');

function readLayoutSubtrees(path) {
  const sourceText = readFileSync(resolve(root, path), 'utf8');
  const source = ts.createSourceFile(path, sourceText, ts.ScriptTarget.Latest, true, ts.ScriptKind.TSX);
  const signatures = [];

  function build(node) {
    const opening = ts.isJsxElement(node) ? node.openingElement : node;
    const tag = opening.tagName.getText(source);
    const layoutAttributes = [];
    for (const property of opening.attributes.properties) {
      if (ts.isJsxAttribute(property) && ['className', 'role', 'aria-label'].includes(property.name.getText(source))) {
        layoutAttributes.push([property.name.getText(source), property.initializer?.getText(source) ?? '']);
      }
    }
    const children = ts.isJsxElement(node)
      ? node.children.filter(child => ts.isJsxElement(child) || ts.isJsxSelfClosingElement(child)).map(build)
      : [];
    return [tag, layoutAttributes, children];
  }

  function visit(node) {
    if (ts.isJsxElement(node) || ts.isJsxSelfClosingElement(node)) signatures.push(JSON.stringify(build(node)));
    ts.forEachChild(node, visit);
  }
  visit(source);
  return signatures;
}

const [baselinePath, currentPath] = process.argv.slice(2);
if (!baselinePath || !currentPath) {
  console.error('Usage: node scripts/measure-jsx-layout-rewrite.mjs <baseline.tsx> <current.tsx>');
  process.exit(2);
}

const baseline = readLayoutSubtrees(baselinePath);
const currentCounts = new Map();
for (const signature of readLayoutSubtrees(currentPath)) currentCounts.set(signature, (currentCounts.get(signature) ?? 0) + 1);
let unchanged = 0;
for (const signature of baseline) {
  const remaining = currentCounts.get(signature) ?? 0;
  if (remaining > 0) {
    unchanged += 1;
    currentCounts.set(signature, remaining - 1);
  }
}
const changed = baseline.length - unchanged;
console.log(`${currentPath}: ${changed}/${baseline.length} original JSX layout subtrees changed (${(changed / baseline.length * 100).toFixed(1)}%)`);
