package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import java.util.List;

public record EnvidoSnapshot(List<EnvidoCall> chain, boolean resolved) {

}
