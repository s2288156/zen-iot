import LogicFlow from '@logicflow/core';

class NodePanel {
  lf: LogicFlow;
  panelEl: HTMLDivElement;
  static pluginName = 'nodePanel';
  domContainer: HTMLDivElement;

  constructor({ lf }) {
    this.lf = lf;
    console.log('domContainer ', this.domContainer)
  }

  render(lf, domContainer) {
    this.panelEl = document.createElement('div');
    this.panelEl.textContent = 'ahahahaha';
    this.panelEl.className = 'node-panel';
    this.panelEl.style.width = '100px';
    this.panelEl.style.height = '100px';
    this.panelEl.style.backgroundColor = 'gray';
    this.panelEl.style.position = 'absolute';
    this.panelEl.style.right = '0px';
    this.panelEl.style.bottom = '0px';

    domContainer.appendChild(this.panelEl);
    this.domContainer = domContainer;
  }
}

export { NodePanel };
