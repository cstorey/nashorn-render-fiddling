import React from 'react';

export { renderToStaticMarkup } from 'react-dom/server';

export function component (name, num) {
  return <dl>
    <dt>name</dt><dd>{name}</dd>
    <dt>num</dt><dd>{num}</dd>
  </dl>;
}
