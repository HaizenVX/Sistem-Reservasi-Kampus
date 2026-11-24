/* Efek antarmuka saja; tidak mengubah data atau keputusan proses. */
window.UiMotion = (() => {
  const reduced = matchMedia('(prefers-reduced-motion: reduce)');
  const pending = new WeakMap(), active = new Set();
  const nativeTransitions = 'onpagereveal' in window && 'onpageswap' in window;
  document.documentElement.classList.toggle('native-transitions', nativeTransitions);
  window.addEventListener('pageswap', event => {
    if (reduced.matches) event.viewTransition?.skipTransition();
  });
  window.addEventListener('pagereveal', event => {
    if (reduced.matches) event.viewTransition?.skipTransition();
  });

  function loading(button) {
    if (!button || pending.has(button)) return () => {};
    const nodes = [...button.childNodes];
    const oldBusy = button.getAttribute('aria-busy');
    const oldWidth = button.style.minWidth;
    button.style.minWidth = button.getBoundingClientRect().width + 'px';
    button.setAttribute('aria-busy', 'true');
    button.classList.add('is-loading');
    button.replaceChildren(document.createTextNode('Memproses…'));
    pending.set(button, true);
    return () => {
      if (!pending.has(button)) return;
      button.replaceChildren(...nodes);
      button.classList.remove('is-loading');
      if (oldBusy === null) button.removeAttribute('aria-busy');
      else button.setAttribute('aria-busy', oldBusy);
      button.style.minWidth = oldWidth;
      pending.delete(button);
      // Status disabled tetap dikelola handler bisnis, termasuk keputusan final.
    };
  }

  function revealCards(container) {
    // Nama fungsi dipertahankan untuk pemanggil lama. Cukup transisi halaman,
    // sehingga respons data tidak memulai animasi kartu kedua.
    container?.classList.remove('reveal-cards');
  }

  function dataFailed() {
    document.querySelectorAll('[data-loading]').forEach(container => {
      const message = 'Data belum dapat dimuat. Silakan muat ulang halaman.';
      if (container.tagName === 'TBODY') {
        const columns = container.querySelector('td')?.colSpan || 1;
        container.replaceChildren();
        const cell = container.insertRow().insertCell();
        cell.colSpan = columns; cell.textContent = message; cell.className = 'empty-state';
      } else {
        const note = document.createElement('p'); note.className = 'loading-error'; note.textContent = message;
        container.replaceChildren(note);
      }
      container.removeAttribute('data-loading'); container.removeAttribute('aria-busy');
    });
  }

  // Navigasi HTML biasa tetap bekerja, termasuk Ctrl+klik, tab baru, dan Back.
  let navigating = false, recoveryTimer;
  function resetNavigation() {
    navigating = false; clearTimeout(recoveryTimer);
    document.body.classList.remove('is-navigating');
  }
  window.addEventListener('pageshow', resetNavigation);
  document.addEventListener('click', event => {
    const link = event.target.closest?.('a[href]');
    if (!link || event.defaultPrevented || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey || link.hasAttribute('download') || (link.target && link.target !== '_self')) return;
    const target = new URL(link.href, location.href);
    if (!document.body.classList.contains('app-layout') || target.origin !== location.origin || !target.pathname.endsWith('.html') || target.hash) return;
    if (target.href === location.href) {event.preventDefault(); return;}
    if (reduced.matches || nativeTransitions) return;
    event.preventDefault();
    if (navigating) return;
    navigating = true;
    document.body.classList.add('is-navigating');
    setTimeout(() => location.assign(target.href), 200);
    // Pulihkan konten bila navigasi dibatalkan; jangan meninggalkan layar transparan.
    recoveryTimer = setTimeout(resetNavigation, 1800);
  });

  document.addEventListener('DOMContentLoaded', () => {
    const previous = new WeakMap();
    const selector = '.pesan, .notif, .alert-box';
    document.querySelectorAll(selector).forEach(el => previous.set(el, el.textContent));
    const observer = new MutationObserver(records => {
      const messages = new Set();
      for (const record of records) {
        const element = record.target.nodeType === Node.ELEMENT_NODE ? record.target : record.target.parentElement;
        const message = element?.closest(selector);
        if (message) messages.add(message);
      }
      for (const message of messages) {
        const text = message.textContent;
        if (previous.get(message) === text) continue;
        previous.set(message, text);
        message.getAnimations().forEach(animation => animation.cancel());
        if (reduced.matches || !text.trim() || text.trim() === '—') continue;
        const animation = message.animate([{opacity:0,transform:'translateY(4px)'},{opacity:1,transform:'translateY(0)'}], {duration:180,easing:'ease-out'});
        active.add(animation);
        animation.finished.catch(() => {}).finally(() => active.delete(animation));
      }
      document.querySelectorAll('[data-loading]').forEach(container => {
        if (!container.querySelector('.loading-row, .skeleton-card, .skeleton-service')) {
          container.removeAttribute('data-loading'); container.removeAttribute('aria-busy');
        }
      });
    });
    observer.observe(document.body, {childList:true,characterData:true,subtree:true});
  });
  reduced.addEventListener('change', () => {if(reduced.matches) active.forEach(animation => animation.cancel());});
  return {loading, revealCards, dataFailed};
})();
