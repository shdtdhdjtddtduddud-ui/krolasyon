// small progressive enhancements (the site works without JavaScript)
(function () {
  document.querySelectorAll('.menu > button').forEach(function (b) {
    b.addEventListener('click', function (e) {
      e.stopPropagation();
      b.parentElement.classList.toggle('open');
    });
  });
  document.addEventListener('click', function () {
    document.querySelectorAll('.menu.open').forEach(function (m) { m.classList.remove('open'); });
  });
  var burger = document.querySelector('.burger');
  if (burger) burger.addEventListener('click', function () { document.querySelector('.top').classList.toggle('open'); });

  // theme toggle
  var root = document.documentElement;
  try { var th = localStorage.getItem('theme'); if (th) root.setAttribute('data-theme', th); } catch (e) {}
  var tt = document.getElementById('theme-toggle');
  if (tt) tt.addEventListener('click', function () {
    var next = root.getAttribute('data-theme') === 'light' ? 'dark' : 'light';
    root.setAttribute('data-theme', next);
    try { localStorage.setItem('theme', next); } catch (e) {}
  });

  // gallery lightbox
  var lb = document.querySelector('.lightbox');
  if (lb) {
    document.querySelectorAll('.gallery img').forEach(function (img) {
      img.addEventListener('click', function () { lb.querySelector('img').src = img.src; lb.classList.add('on'); });
    });
    lb.addEventListener('click', function () { lb.classList.remove('on'); });
  }

  // reply buttons fill the parent id
  document.querySelectorAll('[data-reply]').forEach(function (b) {
    b.addEventListener('click', function () {
      var f = document.getElementById('comment-form');
      f.querySelector('[name=parent_id]').value = b.getAttribute('data-reply');
      f.querySelector('.replying').textContent = b.getAttribute('data-name');
      f.querySelector('.reply-info').style.display = '';
      f.querySelector('textarea').focus();
    });
  });
  var cancel = document.querySelector('.reply-cancel');
  if (cancel) cancel.addEventListener('click', function () {
    var f = document.getElementById('comment-form');
    f.querySelector('[name=parent_id]').value = '';
    f.querySelector('.reply-info').style.display = 'none';
  });

  // confirm dangerous actions
  document.querySelectorAll('[data-confirm]').forEach(function (el) {
    el.addEventListener('submit', function (e) { if (!confirm(el.getAttribute('data-confirm'))) e.preventDefault(); });
  });

})();
