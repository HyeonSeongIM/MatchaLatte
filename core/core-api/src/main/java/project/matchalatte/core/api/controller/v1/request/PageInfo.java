package project.matchalatte.core.api.controller.v1.request;

public record PageInfo(int page, int size, String sortType, String direction) {
}
