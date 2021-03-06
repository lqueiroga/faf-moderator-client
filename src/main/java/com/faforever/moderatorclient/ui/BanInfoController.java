package com.faforever.moderatorclient.ui;

import com.faforever.commons.api.dto.BanDurationType;
import com.faforever.commons.api.dto.BanLevel;
import com.faforever.moderatorclient.api.FafApiCommunicationService;
import com.faforever.moderatorclient.api.domain.BanService;
import com.faforever.moderatorclient.mapstruct.PlayerMapper;
import com.faforever.moderatorclient.ui.domain.BanInfoFX;
import com.faforever.moderatorclient.ui.domain.BanRevokeDataFX;
import com.faforever.moderatorclient.ui.domain.PlayerFX;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;


@Component
@Scope("prototype")
@Slf4j
public class BanInfoController implements Controller<Pane> {
    private final FafApiCommunicationService fafApi;
    private final BanService banService;
    private final PlayerMapper playerMapper;
    public GridPane root;
    public TextField affectedUserTextField;
    public TextField banAuthorTextField;
    public TextField banReasonTextField;
    public TextField revocationReasonTextField;
    public TextField revocationAuthorTextField;
    public TextField untilTextField;
    public Label untilDateTimeValidateLabel;
    public RadioButton permanentBanRadioButton;
    public RadioButton temporaryBanRadioButton;
    public RadioButton chatOnlyBanRadioButton;
    public RadioButton globalBanRadioButton;
    public Button revokeButton;
    public Label userLabel;
    @Getter
    private BanInfoFX banInfo;
    private Consumer<BanInfoFX> postedListener;

    public BanInfoController(FafApiCommunicationService fafApi, BanService banService, PlayerMapper playerMapper) {
        this.fafApi = fafApi;
        this.banService = banService;
        this.playerMapper = playerMapper;
    }

    public void addPostedListener(Consumer<BanInfoFX> listener) {
        this.postedListener = listener;
    }

    @Override
    public Pane getRoot() {
        return root;
    }

    @FXML
    public void initialize() {
    }

    public void setBanInfo(BanInfoFX banInfo) {
        this.banInfo = banInfo;

        if (banInfo.getId() != null) {
            affectedUserTextField.setText(banInfo.getPlayer().representationProperty().get());
            Optional.ofNullable(banInfo.getAuthor()).ifPresent(author -> banAuthorTextField.setText(author.representationProperty().get()));
            banReasonTextField.setText(banInfo.getReason());

            revocationReasonTextField.setDisable(false);
            revokeButton.setDisable(false);

            permanentBanRadioButton.setSelected(banInfo.getDuration() == BanDurationType.PERMANENT);
            temporaryBanRadioButton.setSelected(banInfo.getDuration() == BanDurationType.TEMPORARY);
            Optional.ofNullable(banInfo.getExpiresAt()).ifPresent(offsetDateTime -> untilTextField.setText(offsetDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

            if (banInfo.getBanRevokeData() != null) {
                revocationReasonTextField.setText(banInfo.getBanRevokeData().getReason());
                revocationAuthorTextField.setText(banInfo.getBanRevokeData().getAuthor().toString());
            }

            chatOnlyBanRadioButton.setSelected(banInfo.getLevel() == BanLevel.CHAT);
            globalBanRadioButton.setSelected(banInfo.getLevel() == BanLevel.GLOBAL);
        } else {
            PlayerFX player = banInfo.getPlayer();
            if (player != null) {
                affectedUserTextField.setText(player.representationProperty().get());
            } else {
                affectedUserTextField.setEditable(true);
                affectedUserTextField.setDisable(false);
                userLabel.setText("Affected User ID");
            }
        }
    }

    public void onSave() {
        Assert.notNull(banInfo, "You can't save if banInfo is null.");

        if (!validate()) {
            return;
        }
        ZoneId zoneId = ZoneOffset.systemDefault();
        if (banInfo.getPlayer() == null) {
            PlayerFX playerFX = new PlayerFX();
            playerFX.setId(affectedUserTextField.getText());
            banInfo.setPlayer(playerFX);
        }

        banInfo.setReason(banReasonTextField.getText());
        banInfo.setExpiresAt(temporaryBanRadioButton.isSelected() ?
                OffsetDateTime.of(LocalDateTime.parse(untilTextField.getText(), DateTimeFormatter.ISO_LOCAL_DATE_TIME), ZoneOffset.UTC) : null);
        banInfo.setLevel(chatOnlyBanRadioButton.isSelected() ? BanLevel.CHAT : BanLevel.GLOBAL);

        if (banInfo.getId() == null) {
            log.debug("Creating ban for player '{}' with reason: {}", banInfo.getPlayer().toString(), banReasonTextField.getText());
            String newBanId = banService.createBan(banInfo);
            BanInfoFX loadedBanInfo = banService.getBanInfoById(newBanId);
            if (postedListener != null) {
                postedListener.accept(loadedBanInfo);
            }
        } else {
            log.debug("Updating ban id '{}'", banInfo.getId());
            banService.patchBanInfo(banInfo);
        }
        close();
    }

    private boolean validate() {
        List<String> validationErrors = new ArrayList<>();

        if (banInfo.getPlayer() == null) {
            try {
                Integer.parseInt(affectedUserTextField.getText());
            } catch (Exception e) {
                validationErrors.add("You must specify an affected user");
            }
        }

        if (StringUtils.isBlank(banReasonTextField.getText())) {
            validationErrors.add("No ban reason is given.");
        }

        if (!temporaryBanRadioButton.isSelected() && !permanentBanRadioButton.isSelected()) {
            validationErrors.add("No ban duration is selected.");
        }

        if (!chatOnlyBanRadioButton.isSelected() && !globalBanRadioButton.isSelected()) {
            validationErrors.add("No ban type is selected.");
        }

        if (temporaryBanRadioButton.isSelected()) {
            try {
                LocalDateTime.parse(untilTextField.getText(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException e) {
                validationErrors.add("Expiration date of ban is invalid.");
            }
        }

        if (validationErrors.size() > 0) {
            ViewHelper.errorDialog("Validation failed",
                    validationErrors.stream()
                            .collect(Collectors.joining("\n"))
            );

            return false;
        }

        return true;
    }

    public void onRevoke() {
        Assert.notNull(banInfo, "You can't revoke if banInfo is null.");


        String revocationReason = revocationReasonTextField.getText();

        if (StringUtils.isBlank(revocationReason)) {
            new Alert(Alert.AlertType.ERROR, "The reason of revocation must not be empty", ButtonType.OK).showAndWait();
            return;
        }

        log.debug("Revoking ban id '{}' with reason: {}", banInfo.getId(), revocationReason);

        BanRevokeDataFX banRevokeData = new BanRevokeDataFX()
                .setBan(banInfo)
                .setAuthor(playerMapper.map(fafApi.getSelfPlayer()))
                .setReason(revocationReason);

        banService.revokeBan(banRevokeData);
        close();
    }

    public void onAbort() {
        close();
    }

    public void close() {
        Stage stage = (Stage) root.getScene().getWindow();
        stage.close();
    }

    public void onDurationTextChange() {
        if (untilTextField.getText().length() == 0) {
            untilDateTimeValidateLabel.setText("");
            return;
        }

        try {
            LocalDateTime dateTime = LocalDateTime.parse(untilTextField.getText(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            untilDateTimeValidateLabel.setText("valid");
            untilDateTimeValidateLabel.setStyle("-fx-text-fill: green");
        } catch (DateTimeParseException e) {
            untilDateTimeValidateLabel.setText("invalid");
            untilDateTimeValidateLabel.setStyle("-fx-text-fill: red");
        }
    }
}
