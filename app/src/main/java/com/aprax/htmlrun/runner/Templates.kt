package com.aprax.htmlrun.runner

object Templates {

    val html = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <title>My Page</title>
        </head>
        <body>
          <main class="hero">
            <h1>Hello, world</h1>
            <p>Edit the HTML, CSS and JS tabs, then press Run.</p>
            <button id="btn">Click me</button>
            <p class="out">Clicks: <span id="count">0</span></p>
          </main>
        </body>
        </html>
    """.trimIndent()

    val css = """
        :root { --accent: #4f9dff; }

        * { box-sizing: border-box; }

        body {
          margin: 0;
          min-height: 100vh;
          display: grid;
          place-items: center;
          font-family: system-ui, sans-serif;
          background: radial-gradient(circle at 30% 20%, #1b2340, #0d1017 70%);
          color: #e6ecf7;
        }

        .hero { text-align: center; padding: 24px; }

        h1 {
          font-size: clamp(2rem, 8vw, 4rem);
          margin: 0 0 10px;
          background: linear-gradient(90deg, var(--accent), #a78bfa);
          -webkit-background-clip: text;
          background-clip: text;
          color: transparent;
        }

        button {
          margin-top: 14px;
          font: inherit;
          padding: 10px 22px;
          border: 0;
          border-radius: 999px;
          background: var(--accent);
          color: #08111f;
          font-weight: 700;
          cursor: pointer;
          transition: transform .12s, box-shadow .12s;
        }

        button:hover {
          transform: translateY(-2px);
          box-shadow: 0 8px 24px rgba(79,157,255,.35);
        }

        .out { color: #93a1bb; }
    """.trimIndent()

    val js = """
        const btn = document.getElementById('btn');
        const count = document.getElementById('count');
        let clicks = 0;

        btn.addEventListener('click', () => {
          clicks++;
          count.textContent = clicks;
          console.log('button clicked', clicks, { at: new Date().toLocaleTimeString() });
        });

        console.log('script.js loaded');
        console.info('Console output appears in the panel below the preview.');
    """.trimIndent()
}