package com.villo.truco.domain.shared.pagination;

import java.util.List;

public record CursorPageResult<T>(List<T> items, String nextCursor) {

  public CursorPageResult {

    items = List.copyOf(items);
  }

}
