local repositorio = 'https://github.com/pablovns/petrilens-station/'

function Link(link)
  local alvo = link.target
  if alvo:match('^%a[%w+.-]*:') or alvo:match('^[/#]') then
    return
  end

  local caminho, ancora = alvo:match('^([^#]+)(.*)$')
  if not caminho then
    return
  end

  -- Os caminhos no Markdown são relativos a docs/relatorio.md.
  local partes = {'docs'}
  for parte in caminho:gmatch('[^/]+') do
    if parte == '..' then
      table.remove(partes)
    elseif parte ~= '.' then
      table.insert(partes, parte)
    end
  end

  local tipo = caminho:sub(-1) == '/' and 'tree' or 'blob'
  link.target = repositorio .. tipo .. '/integracao/' .. table.concat(partes, '/') .. ancora
  return link
end
