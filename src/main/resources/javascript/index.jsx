import React from 'react';

export { renderToStaticMarkup } from 'react-dom/server';

export function component (model) {
  return <dl>
    <dt>name</dt><dd>{model.name}</dd>
    <dt>num</dt><dd>{model.num}</dd>
  </dl>;
}
