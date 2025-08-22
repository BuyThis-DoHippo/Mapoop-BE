package BuyThisDoHippo.Mapoop.domain.toilet.controller;

import BuyThisDoHippo.Mapoop.domain.image.dto.ImageInfo;
import BuyThisDoHippo.Mapoop.domain.image.dto.ImageSavedDto;
import BuyThisDoHippo.Mapoop.domain.image.dto.UploadImageResponse;
import BuyThisDoHippo.Mapoop.domain.image.service.ImageCommandService;
import BuyThisDoHippo.Mapoop.domain.image.service.S3ImageService;
import BuyThisDoHippo.Mapoop.domain.toilet.dto.ToiletDetailResponse;
import BuyThisDoHippo.Mapoop.domain.toilet.dto.ToiletRegisterRequest;
import BuyThisDoHippo.Mapoop.domain.toilet.dto.ToiletRegisterResponse;
import BuyThisDoHippo.Mapoop.domain.toilet.dto.ToiletUpdateRequest;
import BuyThisDoHippo.Mapoop.domain.toilet.service.ToiletService;
import BuyThisDoHippo.Mapoop.global.common.CommonResponse;
import BuyThisDoHippo.Mapoop.global.error.ApplicationException;
import BuyThisDoHippo.Mapoop.global.error.CustomErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/toilets")
public class ToiletController {
    private final ToiletService toiletService;
    private final S3ImageService s3ImageService;    // S3 업로드 & 퍼블릭 URL 생성
    private final ImageCommandService imageCommandService;  // DB 저장

    @PostMapping("")
    public CommonResponse<ToiletRegisterResponse> registerToilet(@Valid @RequestBody ToiletRegisterRequest request) {

        // 사용자 정보 가져오기
        Long userId;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApplicationException(CustomErrorCode.AUTHENTICATION_REQUIRED);
        }
        // userId 가져오기
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
           userId = (Long) principal;
        } else {
            throw new ApplicationException(CustomErrorCode.INVALID_TOKEN);
        }

        ToiletRegisterResponse response = toiletService.createToilet(request, userId);
        return CommonResponse.onSuccess(response, "화장실 등록 성공");
    }

    @GetMapping("/{toiletId}")
    public CommonResponse<ToiletDetailResponse> getDetailToilet(@PathVariable Long toiletId) {
        ToiletDetailResponse response = toiletService.getToiletDetail(toiletId);
        return CommonResponse.onSuccess(response, "화장실 상세 조회 성공");
    }

    @PutMapping("/{toiletId}")
    public CommonResponse<Void> updateToilet(
            @PathVariable Long toiletId,
            @Valid @RequestBody ToiletUpdateRequest request,
            Authentication authentication
    ) {
        // 로그인 유저 정보 가져오기
        Long userId = Long.valueOf(authentication.getName());
        toiletService.updateToilet(toiletId, userId, request);
        return CommonResponse.onSuccess(null, "화장실 정보 수정 성공");
    }

    @PostMapping(value = "/{toiletId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponse<UploadImageResponse> uploadToiletImages(
        @PathVariable Long toiletId,
        @RequestPart("files") List<MultipartFile> files
    ) {
        if (files == null || files.isEmpty()) {
            return CommonResponse.onSuccess(null, "업로드할 파일이 없습니다.");
        }

        // S3 업로드
        final List<ImageSavedDto> savedToS3 = s3ImageService.uploadToiletImagesWithMeta(toiletId, files);

        // DB 저장
        final List<ImageInfo> items = new ArrayList<>(savedToS3.size());
        for (int i = 0; i < savedToS3.size(); i++) {
            ImageSavedDto s3 = savedToS3.get(i);

            Long imageId = imageCommandService.saveToiletImage(
                    toiletId, s3.getUrl(), s3.getS3Key(), s3.getOriginalName(), s3.getSize(), s3.getContentType());

            items.add(new ImageInfo(imageId, s3.getUrl()));
        }

        UploadImageResponse response = new UploadImageResponse(items.size(), items);

        return CommonResponse.onSuccess(response, "화장실 이미지 업로드 성공");
    }

}
