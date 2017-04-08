import ReactDOMServer from 'react-dom/server';
import React from 'react';

export function component () { return <div>Fnord!</div>; }

export function renderToStaticMarkup(it) { return ReactDOMServer.renderToStaticMarkup(it) };
