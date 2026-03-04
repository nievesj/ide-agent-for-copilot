export default class StatusMessage extends HTMLElement {
    static get observedAttributes(): string[] {
        return ['type', 'message'];
    }

    private _init = false;

    connectedCallback(): void {
        if (this._init) return;
        this._init = true;
        this._render();
    }

    private _render(): void {
        const type = this.getAttribute('type') || 'info';
        const msg = this.getAttribute('message') || '';
        this.className = 'status-row ' + type;
        const iconSvg = type === 'error'
            ? '<svg viewBox="0 0 16 16" fill="none"><circle cx="8" cy="8" r="7" stroke="currentColor" stroke-width="1.5"/><path d="M5.5 5.5l5 5M10.5 5.5l-5 5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>'
            : '<svg viewBox="0 0 16 16" fill="none"><circle cx="8" cy="8" r="7" stroke="currentColor" stroke-width="1.5"/><path d="M8 7v4.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/><circle cx="8" cy="4.5" r="0.9" fill="currentColor"/></svg>';
        this.innerHTML = `<span class="status-icon">${iconSvg}</span><span>${this._esc(msg)}</span>`;
    }

    private _esc(s: string): string {
        const d = document.createElement('span');
        d.textContent = s;
        return d.innerHTML;
    }

    attributeChangedCallback(): void {
        if (this._init) this._render();
    }
}
