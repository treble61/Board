package com.example.boards.util;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Apache POI를 사용한 엑셀 파일 검증 유틸리티
 */
public class ExcelValidator {

    /**
     * 파일이 유효한 엑셀 파일인지 검증하고 메타데이터 반환
     *
     * @param inputStream 파일 입력 스트림
     * @param filename 파일명
     * @return 검증 결과 및 메타데이터
     */
    public static Map<String, Object> validateExcelFile(InputStream inputStream, String filename) {
        Map<String, Object> result = new HashMap<>();
        result.put("isValid", false);

        if (filename == null) {
            result.put("error", "파일명이 없습니다.");
            return result;
        }

        String lowerFilename = filename.toLowerCase();
        if (!lowerFilename.endsWith(".xlsx") && !lowerFilename.endsWith(".xls")) {
            result.put("error", "엑셀 파일 형식이 아닙니다. (.xlsx, .xls만 가능)");
            return result;
        }

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            result.put("isValid", true);
            result.put("numberOfSheets", workbook.getNumberOfSheets());
            result.put("fileType", lowerFilename.endsWith(".xlsx") ? "XLSX" : "XLS");

            // 첫 번째 시트의 행 개수 (선택적)
            if (workbook.getNumberOfSheets() > 0) {
                int rowCount = workbook.getSheetAt(0).getPhysicalNumberOfRows();
                result.put("firstSheetRowCount", rowCount);
                result.put("firstSheetName", workbook.getSheetAt(0).getSheetName());
            }

            System.out.println("=== 엑셀 파일 검증 성공 ===");
            System.out.println("파일명: " + filename);
            System.out.println("시트 개수: " + result.get("numberOfSheets"));
            System.out.println("파일 형식: " + result.get("fileType"));

        } catch (Exception e) {
            result.put("error", "엑셀 파일을 읽을 수 없습니다. 파일이 손상되었거나 유효하지 않습니다.");
            System.out.println("ERROR: 엑셀 파일 검증 실패 - " + e.getMessage());
        }

        return result;
    }

    /**
     * 파일 확장자만으로 엑셀 파일인지 체크
     */
    public static boolean isExcelFile(String filename) {
        if (filename == null) {
            return false;
        }
        String lowerFilename = filename.toLowerCase();
        return lowerFilename.endsWith(".xlsx") || lowerFilename.endsWith(".xls");
    }
}
