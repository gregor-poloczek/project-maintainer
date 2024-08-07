import * as Diff2Html from 'diff2html';
import 'diff2html/bundles/css/diff2html.min.css';
import "./diff2html-integration.css"

function renderDiff(targetElement, diffString) {
  const diffHtml = Diff2Html.html(
      diffString,
      {
        outputFormat: "side-by-side",
        drawFileList: false,
        matching: 'lines', colorScheme: 'dark'
      });
  targetElement.innerHTML = diffHtml;
}

window.renderDiff = renderDiff;