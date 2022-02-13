package me.aias.controller;

import com.google.common.collect.Lists;
import io.milvus.Response.SearchResultsWrapper;
import io.milvus.grpc.SearchResults;
import io.milvus.param.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.aias.domain.DataInfoRes;
import me.aias.domain.ResEnum;
import me.aias.domain.ResultRes;
import me.aias.service.DataService;
import me.aias.service.FeatureService;
import me.aias.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 搜索管理
 *
 * @author Calvin
 * @date 2021-12-19
 **/
@Slf4j
@Api(tags = "搜索管理")
@RequestMapping("/api/search")
@RequiredArgsConstructor
@RestController
public class SearchController {
    @Autowired
    private SearchService searchService;
    @Autowired
    private DataService imageService;
    @Autowired
    private FeatureService featureService;

    @Value("${search.collectionName}")
    String collectionName;

    @Value("${image.baseUrl}")
    String baseUrl;

    @GetMapping("/text")
    @ApiOperation(value = "搜索", nickname = "search")
    public ResponseEntity<Object> searchImage(@RequestParam("text") String text, @RequestParam(value = "topK") String topk) {
        // 生成向量
        Integer topK = Integer.parseInt(topk);
        List<Float> vectorToSearch;
        try {
            vectorToSearch = featureService.textFeature(text);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return new ResponseEntity<>(ResultRes.error(ResEnum.MODEL_ERROR.KEY, ResEnum.MODEL_ERROR.VALUE), HttpStatus.OK);
        }

        List<List<Float>> vectorsToSearch = new ArrayList<List<Float>>();
        vectorsToSearch.add(vectorToSearch);

        try {
            // 根据向量搜索
            R<SearchResults> searchResponse = searchService.search(topK, vectorsToSearch);
            SearchResultsWrapper wrapper = new SearchResultsWrapper(searchResponse.getData().getResults());
            List<SearchResultsWrapper.IDScore> scores = wrapper.getIDScore(0);
            List<Long> idList = Lists.transform(scores, (entity) -> {
                return entity.getLongID();
            });
            // 根据ID获取图片信息
            ConcurrentHashMap<String, String> map = imageService.getMap();
            List<DataInfoRes> imageInfoResList = new ArrayList<>();
            for (SearchResultsWrapper.IDScore score : scores) {
                DataInfoRes imageInfoRes = new DataInfoRes();
                imageInfoRes.setScore(score.getScore());
                imageInfoRes.setId(score.getLongID());
                imageInfoRes.setUrl(baseUrl + map.get("" + score.getLongID()));
                imageInfoResList.add(imageInfoRes);
            }

            return new ResponseEntity<>(ResultRes.success(imageInfoResList, imageInfoResList.size()), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return new ResponseEntity<>(ResultRes.error(ResEnum.MILVUS_CONNECTION_ERROR.KEY, ResEnum.MILVUS_CONNECTION_ERROR.VALUE), HttpStatus.OK);
        }
    }
}
